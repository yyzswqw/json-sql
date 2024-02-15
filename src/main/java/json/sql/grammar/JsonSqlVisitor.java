package json.sql.grammar;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.jayway.jsonpath.*;
import json.sql.CurContextProxy;
import json.sql.annotation.UdfParser;
import json.sql.config.TableConfig;
import json.sql.entity.TableContext;
import json.sql.entity.UdfFunctionDescInfo;
import json.sql.entity.UdfParamDescInfo;
import json.sql.enums.MacroEnum;
import json.sql.parse.SqlBaseVisitor;
import json.sql.parse.SqlParser;
import json.sql.udf.ListTypeReference;
import json.sql.udf.MapTypeReference;
import json.sql.udf.TypeReference;
import json.sql.util.CompareUtil;
import json.sql.util.CurContext;
import json.sql.util.MacroParamArgsContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class JsonSqlVisitor extends SqlBaseVisitor<Object> {

    /**
     * 所有udf 函数
     */
    private Table<String,Method,List<Class<?>>> methodTable = HashBasedTable.create();
    /**
     * udf 函数中的可变参数(list、map)的泛型（一个udf函数有且仅有一个可变参数）
     */
    private Table<String,Class<?>,TypeReference> variableArgsTypeTable = HashBasedTable.create();
    /**
     * udf 函数宏信息
     */
    private Map<String,List<MacroEnum>> macroMap = Maps.newHashMap();

    /**
     * udf函数描述信息
     */
    private Map<String, UdfFunctionDescInfo> udfFunctionDescInfoMap = Maps.newHashMap();
    /**
     * 所有表数据
     */
    private Map<String, TableContext> tableDataMap = Maps.newHashMap();
    /**
     * 当前操作的表的栈
     */
    private Stack<String> tableNameStack = new Stack<>();
    /**
     * 主表表名
     */
    private static final String MAIN_TABLE_NAME = "__$$main_table_name$$__";

    private static final Pattern colNamePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    /**
     *  udf 函数中的list类型可变参数的默认泛型
     */
    private static final TypeReference defaultListTypeReference = new ListTypeReference(Object.class);
    /**
     *  udf 函数中的map类型可变参数的默认泛型
     */
    private static final TypeReference defaultMapTypeReference = new MapTypeReference(Object.class,Object.class);

    // region ======================== api start ===================================

    /**
     * 注册自定义UDF函数
     * @param functionName 函数名
     * @param method 实现的具体方法，只能是静态方法
     * @param macros 宏参数列表
     * @param argsType 参数列表，不包含宏参数
     */
    public void registerFunction(String functionName,Method method,MacroEnum[] macros,Class<?>[] argsType){
        this.registerFunction(functionName,method,argsType);
        this.registerMacro(functionName,macros);
    }

    /**
     * 注册udf函数描述信息
     * @param functionName 函数名
     * @param functionDescInfo 描述信息
     */
    public void registerFunctionDescInfo(String functionName,UdfFunctionDescInfo functionDescInfo){
        if(ObjectUtil.isEmpty(functionName) || ObjectUtil.isEmpty(functionDescInfo)){
            return;
        }else{
            if (this.udfFunctionDescInfoMap.containsKey(functionName)) {
                throw new RuntimeException("已存在 function : "+functionName +" 描述信息");
            }
            this.udfFunctionDescInfoMap.put(functionName,functionDescInfo);
        }
    }

    /**
     * 获取udf函数描述信息
     * @param functionName 函数名
     * @return udf函数描述信息
     */
    public UdfFunctionDescInfo getFunctionDescInfo(String functionName){
        if(ObjectUtil.isEmpty(functionName)){
            return null;
        }
        UdfFunctionDescInfo descInfo = this.udfFunctionDescInfoMap.get(functionName);
        if(ObjectUtil.isEmpty(descInfo)){
            Map<Method, List<Class<?>>> row = this.methodTable.row(functionName);
            if(ObjectUtil.isEmpty(row)){
                return null;
            }
            Method method = row.keySet().stream().findFirst().orElse(null);
            return UdfParser.getUdfDescInfo(method);
        }
        return descInfo;
    }

    /**
     * 获取所有udf函数描述信息
     * @return udf函数描述信息
     */
    public Collection<UdfFunctionDescInfo> getAllFunctionDescInfo(){
        Collection<UdfFunctionDescInfo> result = new ArrayList<>();
        Map<String, Map<Method, List<Class<?>>>> rowedMap = this.methodTable.rowMap();
        if(ObjectUtil.isEmpty(rowedMap)){
            return result;
        }
        for (String functionName : rowedMap.keySet()) {
            UdfFunctionDescInfo functionDescInfo = getFunctionDescInfo(functionName);
            if(ObjectUtil.isNotEmpty(functionDescInfo)){
                result.add(functionDescInfo);
            }
        }
        return result;
    }

    /**
     * 获取所有注册的表名
     * @return  所有注册的表名
     */
    public Set<String>  getAllTableName(){
        return this.tableDataMap.keySet();
    }

    /**
     * 打印一个 udf 函数描述信息
     */
    public String showUdfDesc(String functionName){
        UdfFunctionDescInfo functionDescInfo = getFunctionDescInfo(functionName);
        StringBuilder sb = new StringBuilder();
        if(ObjectUtil.isEmpty(functionDescInfo)){
            sb.append("not has udf function : ").append(functionName);
            return sb.toString();
        }
        String functionName1 = functionDescInfo.getFunctionName();
        String functionDesc = functionDescInfo.getFunctionDesc();
        String returnType = functionDescInfo.getReturnType();
        List<UdfParamDescInfo> udfParamDescInfoList = functionDescInfo.getUdfParamDescInfoList();

        sb.append(functionName1).append("\n\tdesc: ").append(functionDesc)
                .append("\n\tReturns: ").append(returnType)
                .append("\n\targs:\n");
        if(ObjectUtil.isEmpty(udfParamDescInfoList)){
            sb.append("\t\tNone\n");
        }
        for (UdfParamDescInfo paramDescInfo : udfParamDescInfoList) {
            String paramName = paramDescInfo.getParamName();
            String paramType = paramDescInfo.getParamType();
            String paramDesc = paramDescInfo.getParamDesc();
            sb.append("\t\t").append(paramName)
                    .append("\n\t\t\t").append(String.format("%-15s", paramType))
                    .append("\t").append(paramDesc).append("\n");
        }
        return sb.toString();
    }

    /**
     * 打印所有udf 函数描述信息
     */
    public String showUdfDesc(){
        Collection<UdfFunctionDescInfo> allFunctionDescInfo = getAllFunctionDescInfo();
        StringBuilder sb = new StringBuilder();
        if(ObjectUtil.isEmpty(allFunctionDescInfo)){
            sb.append("not has udf function");
            return sb.toString();
        }
        for (UdfFunctionDescInfo descInfo : allFunctionDescInfo) {
            String functionName = descInfo.getFunctionName();
            String functionDesc = descInfo.getFunctionDesc();
            String returnType = descInfo.getReturnType();
            List<UdfParamDescInfo> udfParamDescInfoList = descInfo.getUdfParamDescInfoList();

            sb.append(functionName).append("\n\tdesc: ").append(functionDesc)
                    .append("\n\tReturns: ").append(returnType)
                    .append("\n\targs:\n");
            if(ObjectUtil.isEmpty(udfParamDescInfoList)){
                sb.append("\t\tNone\n");
            }
            for (UdfParamDescInfo paramDescInfo : udfParamDescInfoList) {
                String paramName = paramDescInfo.getParamName();
                String paramType = paramDescInfo.getParamType();
                String paramDesc = paramDescInfo.getParamDesc();
                sb.append("\t\t").append(paramName)
                    .append("\n\t\t\t").append(String.format("%-15s", paramType))
                    .append("\t").append(paramDesc).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 注册自定义UDF函数
     * @param functionName 函数名
     * @param method 实现的具体方法，只能是静态方法
     * @param macros 宏参数列表
     * @param argsType 参数列表，不包含宏参数
     * @param variableArgsType 可变参数类型
     * @param genericityArgsType 可变参数泛型类型
     */
    public void registerFunction(String functionName,Method method,MacroEnum[] macros,Class<?>[] argsType, Class<?> variableArgsType, TypeReference genericityArgsType){
        this.registerFunction(functionName,method,argsType);
        this.registerMacro(functionName,macros);
        if(ObjectUtil.isAllNotEmpty(variableArgsType,genericityArgsType)){
            this.registerFunctionVariableArgsType(functionName,variableArgsType,genericityArgsType);
        }
    }

    /**
     * 注册自定义UDF函数
     * @param functionName 函数名
     * @param method 实现的具体方法，只能是静态方法
     * @param argsType 参数列表，不包含宏参数，宏参数通过 registerMacro(String functionName,MacroEnum ... macros); 注册
     */
    public void registerFunction(String functionName,Method method,Class<?> ... argsType){
        if(argsType == null || argsType.length == 0){
            this.methodTable.put(functionName,method,new ArrayList<>());
        }else{
            if (this.methodTable.containsRow(functionName)) {
                throw new RuntimeException("已存在 function : "+functionName);
            }
            this.methodTable.put(functionName,method,Arrays.asList(argsType));
        }
    }

    /**
     * 注册函数中可变参数的泛型类型
     * @param functionName 函数名
     * @param argsType 外层参数类型
     * @param genericityArgsType 泛型类型
     */
    public void registerFunctionVariableArgsType(String functionName, Class<?> argsType, TypeReference genericityArgsType){
        if(argsType == null || genericityArgsType == null ){
            throw new RuntimeException("参数不正确 function : "+functionName);
        }else{
            if (this.variableArgsTypeTable.containsRow(functionName)) {
                throw new RuntimeException("已存在 function : "+functionName +" 可变参数泛型定义");
            }
            this.variableArgsTypeTable.put(functionName,argsType,genericityArgsType);
        }
    }

    /**
     * 获取函数中可变参数的泛型类型
     * @param functionName 函数名
     * @param argsType 外层参数类型
     * @return 泛型类型
     */
    public TypeReference getFunctionVariableArgsType(String functionName, Class<?> argsType){
        if(argsType == null || functionName == null ){
            throw new RuntimeException("参数不正确");
        }else{
            TypeReference typeReference = this.variableArgsTypeTable.get(functionName, argsType);
            if(ObjectUtil.isNotEmpty(typeReference)){
                return typeReference;
            }
            if(Collection.class.isAssignableFrom(argsType)){
                return defaultListTypeReference;
            }
            if(Map.class.isAssignableFrom(argsType)){
                return defaultMapTypeReference;
            }
            return null;
        }
    }


    /**
     * 注册函数宏参数，宏参数必须定义在函数的最开始的参数
     * @param functionName 函数名
     * @param macros 宏参数列表
     */
    public void registerMacro(String functionName,MacroEnum ... macros) {
        if(macros == null || macros.length == 0){
            return ;
        }else{
            if (this.macroMap.containsKey(functionName)) {
                throw new RuntimeException("已存在 macro : "+functionName);
            }
            this.macroMap.put(functionName,Arrays.asList(macros));
        }
    }

    /**
     * 获取 udf 函数参数列表
     * @param functionName 函数名
     * @return 参数列表
     */
    public Map<String,List<Class<?>>> getUdfFunction(String functionName){
        Map<String,List<Class<?>>> result = new HashMap<>();
        if(ObjectUtil.isEmpty(functionName)){
            return result;
        }
        Map<Method, List<Class<?>>> row = this.methodTable.row(functionName);
        if(ObjectUtil.isEmpty(row)){
            return result;
        }
        Collection<List<Class<?>>> values = row.values();
        if(ObjectUtil.isEmpty(values)){
            result.put(functionName,new ArrayList<>());
            return result;
        }
        result.put(functionName,values.stream().findFirst().orElse(new ArrayList<>()));
        return result;
    }

    /**
     * 获取所有 udf 函数参数列表
     * @return 函数及其参数列表
     */
    public Map<String,List<Class<?>>> getAllUdfFunction(){
        Map<String,List<Class<?>>> result = new HashMap<>();
        Map<String, Map<Method, List<Class<?>>>> udfMethodMap = this.methodTable.rowMap();
        if(ObjectUtil.isEmpty(udfMethodMap)){
            return result;
        }
        for (Map.Entry<String, Map<Method, List<Class<?>>>> udfMethodMapEntry : udfMethodMap.entrySet()) {
            String functionName = udfMethodMapEntry.getKey();
            Map<Method, List<Class<?>>> udfMap = udfMethodMapEntry.getValue();
            Collection<List<Class<?>>> values = udfMap.values();
            if(ObjectUtil.isEmpty(values)){
                result.put(functionName,new ArrayList<>());
                continue;
            }
            result.put(functionName,values.stream().findFirst().orElse(new ArrayList<>()));
        }
        return result;
    }

    /**
     * 删除表
     * @param tableName 表名
     */
    public Integer dropTable(String tableName) {
        if(ObjectUtil.hasEmpty(tableName)){
            throw new RuntimeException("表名不能为空");
        }
        TableContext tableContext = this.tableDataMap.remove(tableName);
        if(ObjectUtil.isNotEmpty(tableContext)){
            tableContext.setDocument(null);
            tableContext.setNewDocument(null);
            tableContext.setOriginalJson(null);
            return 1;
        }
        return 0;
    }

    /**
     * 注册表
     * @param tableName 表名
     * @param json json值
     * @param config 配置项
     */
    public void registerTable(String tableName,String json,Map<String,Object> config) {
        if(ObjectUtil.hasEmpty(tableName,json)){
            throw new RuntimeException("表名或者json 数据不能为空");
        }
        TableContext tableContext = this.tableDataMap.get(tableName);
        if(ObjectUtil.isNotEmpty(tableContext)){
            throw new RuntimeException("table is exist");
        }
        tableContext = new TableContext();
        tableContext.setOriginalJson(json);
        tableContext.setDocument(JsonPath.parse(json));
        tableContext.setNewDocument(JsonPath.parse(json));
        if(ObjectUtil.isNotEmpty(config)){
            tableContext.getConfig().putAll(config);
        }
        this.tableDataMap.put(tableName,tableContext);
    }

    /**
     * 注册表
     * @param tableName 表名
     * @param json json值
     */
    public void registerTable(String tableName,String json) {
        this.registerTable(tableName,json,null);
    }

    /**
     * 设置表的配置项
     * @param tableName 表名
     * @param key 配置项key
     * @param value 值
     */
    public void setTableConfig(String tableName,String key,Object value) {
        Map<String, Object> tableContextConfig = this.getTableContextConfig(tableName);
        if(tableContextConfig != null){
            tableContextConfig.put(key,value);
        }
    }

    /**
     * 执行sql语法树
     * @param tree sql的语法树
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(ParseTree tree){
        return this.exec(tree,null,null);
    }

    /**
     * 执行sql语法树
     * @param tree sql的语法树
     * @param jsonString json值
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(ParseTree tree,String jsonString){
        return this.exec(tree,null,jsonString);
    }

    /**
     * 执行sql语法树
     * @param tree sql的语法树
     * @param writeModel 写模式
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(ParseTree tree,Boolean writeModel){
        return this.exec(tree,writeModel,null);
    }

    /**
     * 执行sql语法树
     * @param tree sql的语法树
     * @param writeModel 写模式
     * @param jsonString json值
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(ParseTree tree,Boolean writeModel,String jsonString){
        if(ObjectUtil.isNotEmpty(jsonString)){
            this.setJsonString(jsonString);
        }
        if(ObjectUtil.isNotEmpty(writeModel)){
            this.setWriteModel(MAIN_TABLE_NAME,writeModel);
        }
        Object visit = this.visit(tree);
        if(ObjectUtil.isNotEmpty(visit)){
            return visit.toString();
        }
        return this.getResult(MAIN_TABLE_NAME);
    }

    /**
     * 执行sql
     * @param sql sql
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(String sql){
        return this.exec(sql,false,null);
    }

    /**
     * 执行sql
     * @param sql sql
     * @param jsonString json值
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(String sql,String jsonString){
        return this.exec(sql,false,jsonString);
    }

    /**
     * 执行sql
     * @param sql sql
     * @param writeModel 写模式
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(String sql,boolean writeModel){
        return this.exec(sql,writeModel,null);
    }

    /**
     * 执行sql
     * @param sql sql
     * @param writeModel 写模式
     * @param jsonString json值
     * @return 执行结果，为空则返回主表结果
     */
    public String exec(String sql,boolean writeModel,String jsonString){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);
        ParseTree tree = parser.sql();
        if (parserErrorListener.hasError()) {
            List<String> errors = parserErrorListener.errors();
            String join = String.join("\n", errors);
            throw new RuntimeException("parser errors : "+join);
        }
        if(ObjectUtil.isNotEmpty(jsonString)){
            this.setJsonString(jsonString);
        }
        if(ObjectUtil.isNotEmpty(writeModel)){
            this.setWriteModel(MAIN_TABLE_NAME,writeModel);
        }
        Object visit = this.visit(tree);
        if(ObjectUtil.isNotEmpty(visit)){
            return visit.toString();
        }
        return this.getResult(MAIN_TABLE_NAME);
    }


    /**
     * 注册主表
     * @param jsonString json值
     */
    public void setJsonString(String jsonString) {
        registerTable(MAIN_TABLE_NAME,jsonString);
    }

    /**
     * 设置表的写模式
     * @param tableName 表名
     * @param writeModel 写模式
     */
    public void setWriteModel(String tableName,boolean writeModel) {
        TableContext tableContext = getTableContext(tableName);
        if(ObjectUtil.isEmpty(tableContext)){
            this.registerTable(tableName,"{}");
        }
        tableContext = getTableContext(tableName);
        tableContext.setConfig(TableConfig.WRITE_MODEL,writeModel);
    }

    /**
     * 获取表的最终json值结果
     * @param tableName 表名
     * @return 表的json值
     */
    public String getResult(String tableName){
        TableContext tableContext = this.tableDataMap.get(tableName);
        if(ObjectUtil.isEmpty(tableContext)){
            return null;
        }
        Boolean writeModel = MapUtil.getBool(tableContext.getConfig(), TableConfig.WRITE_MODEL
                , MapUtil.getBool(TableContext.defaultConfig, TableConfig.WRITE_MODEL, false));
        if(writeModel && tableContext.getNewDocument() != null){
            return tableContext.getNewDocument().jsonString();
        }
        if(!writeModel && tableContext.getDocument() != null){
            return tableContext.getDocument().jsonString();
        }
        return tableContext.getOriginalJson();
    }

    /**
     * 获取表的上下文
     * @param tableName 表名
     * @return 表的上下文
     */
    public TableContext getTableContext(String tableName){
        return this.tableDataMap.get(tableName);
    }

    /**
     * 获取表的所有配置项
     * @param tableName 表名
     * @return  配置项的值
     */
    public Map<String,Object> getTableContextConfig(String tableName){
        TableContext tableContext = this.getTableContext(tableName);
        return tableContext == null?null:tableContext.getConfig();
    }

    /**
     * 获取表的配置项
     * @param tableName 表名
     * @param key 配置项key
     * @param classType 转换的类型
     * @return 配置项的值
     * @param <T> 转换后返回的类型
     */
    public <T> T getTableContextConfig(String tableName,String key,Class<T> classType){
        TableContext tableContext = this.getTableContext(tableName);
        Map<String, Object> config = tableContext.getConfig();
        T v = null;
        if(ObjectUtil.isNotEmpty(config)){
            v = MapUtil.get(config, key, classType);
            if(v != null){
                return v;
            }
        }
        v = MapUtil.get(TableContext.defaultConfig, key, classType);
        return v;
    }

    /**
     * 获取表的配置项
     * @param tableName 表名
     * @param key 配置项key
     * @param classType 转换的类型
     * @param defaultValue 默认值
     * @return 配置项的值
     * @param <T> 转换后返回的类型
     */
    public <T> T getTableContextConfig(String tableName,String key,Class<T> classType,T defaultValue){
        TableContext tableContext = this.getTableContext(tableName);
        Map<String, Object> config = tableContext.getConfig();
        T v = null;
        if(ObjectUtil.isNotEmpty(config)){
            v = MapUtil.get(config, key, classType);
            if(v != null){
                return v;
            }
        }
        v = MapUtil.get(TableContext.defaultConfig, key, classType);
        if(v != null){
            return v;
        }
        return defaultValue;
    }

    /**
     * 获取原始操作的DocumentContext
     * @param tableName 表名
     * @return DocumentContext
     */
    public DocumentContext getTableContextDocument(String tableName){
        TableContext tableContext = this.getTableContext(tableName);
        return tableContext == null?null:tableContext.getDocument();
    }

    /**
     * 获取当前copy on write操作的DocumentContext
     * @param tableName 表名
     * @return DocumentContext
     */
    public DocumentContext getTableContextNewDocument(String tableName){
        TableContext tableContext = this.getTableContext(tableName);
        return tableContext == null?null:tableContext.getNewDocument();
    }

    /**
     * 获取当前写操作的DocumentContext
     * @param tableName 表名
     * @return DocumentContext
     */
    public DocumentContext getTableContextCurWriteDocument(String tableName){
        TableContext tableContext = this.tableDataMap.get(tableName);
        if(ObjectUtil.isEmpty(tableContext)){
            return null;
        }
        Boolean writeModel = MapUtil.getBool(tableContext.getConfig(), TableConfig.WRITE_MODEL
                , MapUtil.getBool(TableContext.defaultConfig, TableConfig.WRITE_MODEL, false));
        if(writeModel && tableContext.getNewDocument() != null){
            return tableContext.getNewDocument();
        }
        if(!writeModel && tableContext.getDocument() != null){
            return tableContext.getDocument();
        }
        return null;
    }

    /**
     * 获取表的原始json
     * @param tableName 表名
     * @return 原始json
     */
    public String getTableContextOriginalJson(String tableName){
        TableContext tableContext = this.getTableContext(tableName);
        return tableContext == null?null:tableContext.getOriginalJson();
    }

    /**
     * 删除列
     * @param tableName 表名
     * @param jsonPath jsonPath
     * @return 是否删除成功 1:是，0::否
     */
    public int delCol(String tableName,String jsonPath){
        TableContext tableContext = this.getTableContext(tableName);
        if(ObjectUtil.isEmpty(tableContext)){
            return 0;
        }
        try {
            Boolean writeModel = this.getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
            if(writeModel){
                this.getTableContextNewDocument(tableName).delete(jsonPath);
            }else{
                this.getTableContextDocument(tableName).delete(jsonPath);
            }
            return 1;
        }catch (Exception ignored){
        }
        return 0;
    }

    /**
     * 获取当前操作表的jsonPath的值
     * @param jsonPath jsonPath
     * @return 值
     */
    public Object read(String jsonPath){
        try {
            String tableName = this.tableNameStack.peek();
            return getTableContextDocument(tableName).read(jsonPath);
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 根据表名和jsonPath 获取值,通过当前操作的JsonDocument
     * @param tableName 表名
     * @param jsonPath jsonPath
     * @return 值
     */
    public Object readCurWriteDocument(String tableName,String jsonPath){
        try {
            return getTableContextCurWriteDocument(tableName).read(jsonPath);
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 根据表名和jsonPath 获取值,通过原始的JsonDocument
     * @param tableName 表名
     * @param jsonPath jsonPath
     * @return 值
     */
    public Object readOriginalDocument(String tableName,String jsonPath){
        try {
            return getTableContextDocument(tableName).read(jsonPath);
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 根据表名和jsonPath 获取值,通过 copy on write 的JsonDocument
     * @param tableName 表名
     * @param jsonPath jsonPath
     * @return 值
     */
    public Object readCopyOnWriteDocument(String tableName,String jsonPath){
        try {
            return getTableContextNewDocument(tableName).read(jsonPath);
        }catch (Exception e){
            return null;
        }
    }

// endregion ======================== api end ===================================

    @Override
    public Object visitSql(SqlParser.SqlContext ctx) {
        List<SqlParser.SqlStatementContext> sqlStatementContexts = ctx.sqlStatement();
        if(ObjectUtil.isEmpty(sqlStatementContexts)){
            return null;
        }
        Object lastResult = null;
        for (SqlParser.SqlStatementContext sqlStatementContext : sqlStatementContexts) {
            if(ObjectUtil.isEmpty(sqlStatementContext)){
                continue;
            }
            lastResult = visitSqlStatement(sqlStatementContext);
        }
        return lastResult;
    }

    @Override
    public Object visitSqlStatement(SqlParser.SqlStatementContext ctx) {
        SqlParser.UpdateStatementContext updateStatementContext = ctx.updateStatement();
        SqlParser.SelectStatementContext selectStatementContext = ctx.selectStatement();
        SqlParser.DeleteStatementContext deleteStatementContext = ctx.deleteStatement();
        SqlParser.CreateTableStatementContext createTableStatement = ctx.createTableStatement();
        SqlParser.DropTableStatementContext dropTableStatementContext = ctx.dropTableStatement();

        if(ObjectUtil.isNotEmpty(updateStatementContext)){
            return visitUpdateStatement(updateStatementContext);
        }
        if(ObjectUtil.isNotEmpty(selectStatementContext)){
            return visitSelectStatement(selectStatementContext);
        }
        if(ObjectUtil.isNotEmpty(deleteStatementContext)){
            return visitDeleteStatement(deleteStatementContext);
        }
        if(ObjectUtil.isNotEmpty(createTableStatement)){
            return visitCreateTableStatement(createTableStatement);
        }
        if(ObjectUtil.isNotEmpty(dropTableStatementContext)){
            return visitDropTableStatement(dropTableStatementContext);
        }
        return null;
    }

    // region ======================== create table start ===================================

    @Override
    public Object visitCreateTableStatement(SqlParser.CreateTableStatementContext ctx) {
        SqlParser.TableNameContext tableNameContext = ctx.tableName();
        SqlParser.SelectStatementContext selectStatementContext = ctx.selectStatement();
        Object o = visitSelectStatement(selectStatementContext);
        if(ObjectUtil.isEmpty(o)){
            return 0;
        }
        try {
            this.registerTable(tableNameContext.getText(),o.toString());
        }catch (Exception e){
            return 0;
        }
        return 1;
    }


    // endregion ======================== create table end ===================================

    // region ======================== drop table start ===================================

    @Override
    public Object visitDropTableStatement(SqlParser.DropTableStatementContext ctx) {
        SqlParser.TableNameContext tableNameContext = ctx.tableName();
        if(ObjectUtil.isEmpty(tableNameContext) && ObjectUtil.isEmpty(tableNameContext.getText())){
            return 0;
        }
        return this.dropTable(tableNameContext.getText());
    }


    // region ======================== drop table end ===================================

    // region ======================== update start ===================================
    @Override
    public Object visitUpdateStatement(SqlParser.UpdateStatementContext ctx) {
        SqlParser.TableNameContext tableNameContext = ctx.tableName();
        if(ObjectUtil.isNotEmpty(tableNameContext)){
            String tableName = tableNameContext.getText();
            if (!this.tableDataMap.containsKey(tableName)) {
                throw new RuntimeException("not exist table : "+tableName);
            }
            this.tableNameStack.push(tableName);
        }else{
            this.tableNameStack.push(MAIN_TABLE_NAME);
        }

        // 处理 WHERE 子句
        if (ctx.expression() != null) {
            boolean result = (boolean) visit(ctx.expression());
            if (result) {
                // 处理 SET 子句
                if (ctx.setClause() != null) {
                    visit(ctx.setClause());
                    return 1;
                }
                this.tableNameStack.pop();
                return 0;
            } else {
                this.tableNameStack.pop();
                return 0;
            }
        } else {
            // 处理 SET 子句
            if (ctx.setClause() != null) {
                visit(ctx.setClause());
            }
            this.tableNameStack.pop();
            return 1;
        }
    }

    @Override
    public Object visitSetClause(SqlParser.SetClauseContext ctx) {
        List<SqlParser.SetExpressionContext> setExpressionContexts = ctx.setExpression();

        for (SqlParser.SetExpressionContext setExpression : setExpressionContexts) {
            visit(setExpression);
        }
        return null;
    }

    @Override
    public Object visitSetExpression(SqlParser.SetExpressionContext ctx) {
        SqlParser.CustomFunctionContext customFunctionContext = ctx.customFunction();
        if(ObjectUtil.isNotEmpty(customFunctionContext)){
            return visitCustomFunction(customFunctionContext);
        }
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        String jsonPath = (String)visitColumnName(columnNameContext);
        SqlParser.RelationalExprContext relationalExprContext = ctx.relationalExpr();
        SqlParser.CaseExprContext caseExprContext = ctx.caseExpr();
        String tableName = this.tableNameStack.peek();
        Object value = null;
        if(relationalExprContext != null){
            value = visit(relationalExprContext);
        }
        if(caseExprContext != null){
            value = visitCaseExpr(caseExprContext);
        }

        try {
            Boolean writeModel = this.getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
            if(writeModel){
                this.getTableContextNewDocument(tableName).set(jsonPath, value);
            }else{
                this.getTableContextDocument(tableName).set(jsonPath, value);
            }
        }catch (PathNotFoundException e){
            // 没有这个节点，新增
            int i = jsonPath.lastIndexOf(".");
            String firstPath = null;
            String namePath = null;
            if(i>0){
                firstPath = jsonPath.substring(0,i);
                namePath = jsonPath.substring(i+1);
            }else{
                firstPath = "$";
                namePath = jsonPath;
            }
            Boolean writeModel = this.getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
            if(writeModel){
                this.getTableContextNewDocument(tableName).put(firstPath,namePath, value);
            }else{
                this.getTableContextDocument(tableName).put(firstPath,namePath, value);
            }
        }
        return null;
    }

    // endregion ======================== update end ===================================


    // region ======================== expression start ===================================

    @Override
    public Object visitExpression(SqlParser.ExpressionContext ctx) {
        SqlParser.OrExprContext orExprContext = ctx.orExpr();
        if(orExprContext != null){
            return visitOrExpr(orExprContext);
        }
        SqlParser.ExpressionContext expression = ctx.expression();
        if(expression != null){
            return visitExpression(expression);
        }
        return true;
    }

    @Override
    public Object visitOrExpr(SqlParser.OrExprContext ctx) {
        List<SqlParser.AndExprContext> andExprContexts = ctx.andExpr();
        for (SqlParser.AndExprContext andExprContext : andExprContexts) {
            Object visit = visitAndExpr(andExprContext);
            if (visit != null && (Boolean) visit) {
                return true;
            }
        }
        SqlParser.ExpressionContext expression = ctx.expression();
        if(expression != null){
            return visitExpression(expression);
        }
        return false;
    }

    @Override
    public Object visitAndExpr(SqlParser.AndExprContext ctx) {
        List<SqlParser.EqualityExprContext> equalityExprContexts = ctx.equalityExpr();
        for (SqlParser.EqualityExprContext equalityExprContext : equalityExprContexts) {
            Object visit = visitEqualityExpr(equalityExprContext);
            if (visit != null && !((Boolean) visit)) {
                return false;
            }
        }
        SqlParser.ExpressionContext expression = ctx.expression();
        if(expression != null){
            return visitExpression(expression);
        }
        return true;
    }

    @Override
    public Object visitMulDiv(SqlParser.MulDivContext ctx) {
        ParseTree child0 = ctx.getChild(0);
        ParseTree child1 = ctx.getChild(1);
        ParseTree child2 = ctx.getChild(2);
        Object v1 = visit(child0);
        Object v2 = visit(child2);
        if(v1 == null || v2 == null){
            return null;
        }
        BigDecimal sum = new BigDecimal(v1.toString());
        BigDecimal temp = new BigDecimal(v2.toString());
        if(child1.getText().equals("*")){
            sum = sum.multiply(temp);
        }else if(child1.getText().equals("/")){
            if(temp.equals(BigDecimal.ZERO)){
                return null;
            }
            sum = sum.divide(temp, 13,RoundingMode.HALF_UP);
        }else if(child1.getText().equals("%")){
            if(temp.equals(BigDecimal.ZERO)){
                return null;
            }
            sum = sum.divideAndRemainder(temp)[1];
        }
        return sum;
    }

    @Override
    public Object visitAddSub(SqlParser.AddSubContext ctx) {
        ParseTree child0 = ctx.getChild(0);
        ParseTree child1 = ctx.getChild(1);
        ParseTree child2 = ctx.getChild(2);
        Object v1 = visit(child0);
        Object v2 = visit(child2);
        if(v1 == null || v2 == null){
            return null;
        }
        BigDecimal sum = new BigDecimal(v1.toString());
        if(child1.getText().equals("-")){
            v2 = new BigDecimal("0").subtract(new BigDecimal(v2.toString()));
        }
        BigDecimal temp = new BigDecimal(v2.toString());
        sum = sum.add(temp);
        return sum;
    }

    @Override
    public Object visitDoubleValue(SqlParser.DoubleValueContext ctx) {
        String text = ctx.getText();
        return new BigDecimal(text);
    }

    @Override
    public Object visitIntValue(SqlParser.IntValueContext ctx) {
        String text = ctx.getText();
        return new BigDecimal(text);
    }

    @Override
    public Object visitId(SqlParser.IdContext ctx) {
        SqlParser.PrimaryExprContext primaryExprContext = ctx.primaryExpr();
        return visitPrimaryExpr(primaryExprContext);
    }

    @Override
    public Object visitParens(SqlParser.ParensContext ctx) {
        ParseTree child1 = ctx.getChild(1);
        return visit(child1);
    }

    @Override
    public Object visitEqualityExpr(SqlParser.EqualityExprContext ctx) {
        SqlParser.IsNullExpressionContext nullExpression = ctx.isNullExpression();
        if (nullExpression != null) {
            return visitIsNullExpression(nullExpression);
        }

        SqlParser.BoolLableContext boolLableContext = ctx.boolLable();
        if (boolLableContext != null) {
            return visitBoolLable(boolLableContext);
        }

        List<SqlParser.RelationalExprContext> relationalExprContexts = ctx.relationalExpr();
        if (relationalExprContexts != null && !relationalExprContexts.isEmpty()) {
            SqlParser.ComparisonOperatorContext comparisonOperatorContext = ctx.comparisonOperator();
            SqlParser.RelationalExprContext left = relationalExprContexts.get(0);
            Object v1 = visit(left);
            if (relationalExprContexts.size() <= 1) {
                return Boolean.parseBoolean(v1.toString());
            }
            SqlParser.RelationalExprContext right = relationalExprContexts.get(1);
            Object v2 = visit(right);
            String operator = comparisonOperatorContext.getText();
            return CompareUtil.compareValues(v1,operator,v2);
        }
        SqlParser.ExpressionContext expression = ctx.expression();
        if (expression != null) {
            return visitExpression(expression);
        }
        SqlParser.InSubqueryExpressionContext inSubqueryExpressionContext = ctx.inSubqueryExpression();
        if (inSubqueryExpressionContext != null) {
            return visitInSubqueryExpression(inSubqueryExpressionContext);
        }
        SqlParser.ExistsSubqueryExpressionContext existsSubqueryExpressionContext = ctx.existsSubqueryExpression();
        if (existsSubqueryExpressionContext != null) {
            return visitExistsSubqueryExpression(existsSubqueryExpressionContext);
        }
        SqlParser.BetweenExpressionContext betweenExpressionContext = ctx.betweenExpression();
        if (betweenExpressionContext != null) {
            return visitBetweenExpression(betweenExpressionContext);
        }
        SqlParser.LikeExpressionContext likeExpressionContext = ctx.likeExpression();
        if (likeExpressionContext != null) {
            return visitLikeExpression(likeExpressionContext);
        }
        return false;
    }



    @Override
    public Object visitIsNullExpression(SqlParser.IsNullExpressionContext ctx) {
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        // childCount=3:is null ，childCount=4:is not null
        int childCount = ctx.getChildCount();
        try {
            Object read = read(columnNameContext.getText());
            if (read == null) {
                if (childCount == 3) {
                    return true;
                } else if (childCount == 4) {
                    return false;
                }
                return false;
            }
        } catch (Exception e) {
            // 没有这个属性
            if (childCount == 3) {
                return true;
            } else if (childCount == 4) {
                return false;
            }
            return false;
        }

        return false;
    }


    @Override
    public Object visitPrimaryExpr(SqlParser.PrimaryExprContext ctx) {
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        if (columnNameContext != null) {
            try {
                String jsonPath = (String)visitColumnName(columnNameContext);
                return read(jsonPath);
            } catch (Exception e) {
                // 没有这个属性
                e.printStackTrace();
                return null;
            }
        }

        SqlParser.LiteralValueContext literalValueContext = ctx.literalValue();
        if (literalValueContext != null) {
            return visitLiteralValue(literalValueContext);
        }
        SqlParser.ExpressionContext expression = ctx.expression();
        if (expression != null) {
            if (expression.getChildCount() == 3) {
                return visit(expression.getChild(1));
            }
            return visitExpression(expression);
        }
        return null;
    }

    @Override
    public Object visitCaseExpr(SqlParser.CaseExprContext ctx) {
        List<SqlParser.WhenBranchContext> whenBranchContexts = ctx.whenBranch();
        for (SqlParser.WhenBranchContext whenBranchContext : whenBranchContexts) {
            WhenThenResult result = (WhenThenResult)visitWhenBranch(whenBranchContext);
            if(result.getConditionTrue()){
                return result.getResult();
            }
        }
        SqlParser.ElseBranchContext elseBranchContext = ctx.elseBranch();
        if(elseBranchContext != null){
            return visitElseBranch(elseBranchContext);
        }
        return null;
    }

    @Override
    public Object visitWhenBranch(SqlParser.WhenBranchContext ctx) {
        SqlParser.ConditionContext condition = ctx.condition();
        SqlParser.RelationalExprContext expression = ctx.relationalExpr();
        Boolean v1 = (Boolean)visitCondition(condition);
        Object v2 = null;
        if(v1){
            v2 = visit(expression);
        }
        WhenThenResult whenThenResult = new WhenThenResult();
        whenThenResult.setConditionTrue(v1);
        whenThenResult.setResult(v2);
        return whenThenResult;
    }

    @Override
    public Object visitCondition(SqlParser.ConditionContext ctx) {
        SqlParser.ExpressionContext expression = ctx.expression();
        Object v1 = visitExpression(expression);
        return Boolean.parseBoolean(v1==null?"false":v1.toString());
    }

    @Override
    public Object visitIfFunction(SqlParser.IfFunctionContext ctx) {
        List<SqlParser.IfTrueAndResultBranchContext> ifTrueAndResultBranchContexts = ctx.ifTrueAndResultBranch();
        SqlParser.IfFuncElseBranchContext ifFuncElseBranchContext = ctx.ifFuncElseBranch();
        if(ObjectUtil.isNotEmpty(ifTrueAndResultBranchContexts)){
            for (SqlParser.IfTrueAndResultBranchContext ifTrueAndResultBranchContext : ifTrueAndResultBranchContexts) {
                if(ObjectUtil.isNotEmpty(ifTrueAndResultBranchContext)){
                    SqlParser.ExpressionContext expression = ifTrueAndResultBranchContext.expression();
                    SqlParser.RelationalExprContext relationalExprContext = ifTrueAndResultBranchContext.relationalExpr();
                    Object o = visitExpression(expression);
                    if(ObjectUtil.isNotEmpty(o) && Boolean.parseBoolean(o.toString())){
                        return visit(relationalExprContext);
                    }
                }
            }
        }
        if(ObjectUtil.isNotEmpty(ifFuncElseBranchContext)){
            SqlParser.RelationalExprContext relationalExprContext = ifFuncElseBranchContext.relationalExpr();
            if(ObjectUtil.isNotEmpty(relationalExprContext)){
                return visit(relationalExprContext);
            }
        }
        return null;
    }

    // endregion ======================== expression end ===================================

    // region ======================== function start ===================================

    @Override
    public Object visitJsonPathFunc(SqlParser.JsonPathFuncContext ctx) {
        SqlParser.JsonPathFunctionContext jsonPathContext = ctx.jsonPathFunction();
        if(jsonPathContext != null){
            String jsonPath = jsonPathContext.getChild(1).getChild(1).getText();
            return read(jsonPath);
        }
        return null;
    }

    @Override
    public Object visitToJsonByPathFunction(SqlParser.ToJsonByPathFunctionContext ctx) {
        TerminalNode string = ctx.STRING();
        if(string != null){
            try {
                Object read = read(string.getText().substring(1, string.getText().length() - 1));
                DocumentContext parse = JsonPath.parse(read);
                return parse.json();
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    @Override
    public Object visitToJsonFunction(SqlParser.ToJsonFunctionContext ctx) {
        TerminalNode string = ctx.STRING();
        if(string != null){
            try {
                DocumentContext parse = JsonPath.parse(string.getText().substring(1,string.getText().length() - 1));
                return parse.json();
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    @Override
    public Object visitFunctionCall(SqlParser.FunctionCallContext ctx) {
        TerminalNode customId = ctx.CUSTOMID();
        SqlParser.ArgsContext args = ctx.args();
        List<Object> innerArgs = new ArrayList<>();
        if(args != null){
            List<SqlParser.ArgsExpressionContext> argsExpressionContexts = args.argsExpression();
            for (SqlParser.ArgsExpressionContext argsExpressionContext : argsExpressionContexts) {
                Object o = visitArgsExpression(argsExpressionContext);
                innerArgs.add(o);
            }
        }
        String methodName = customId.getText().substring(1);
        Map<Method, List<Class<?>>> row = this.methodTable.row(methodName);
        if(row.isEmpty()){
            throw new RuntimeException("not has function : "+methodName);
        }
        Map.Entry<Method, List<Class<?>>> methodListEntry = row.entrySet().stream().findFirst().get();
        Method method = methodListEntry.getKey();
        List<Class<?>> argsTypeClasses = methodListEntry.getValue();
        List<Object> innerArgsList = new ArrayList<>();
        Object result = null;
        if(method != null){
            List<MacroEnum> macroEnums = this.macroMap.get(methodName);
            if(ObjectUtil.isNotEmpty(macroEnums)){
                for (MacroEnum macroEnum : macroEnums) {
                    innerArgsList.add(getMacro(macroEnum));
                }
            }
            try {
                if(argsTypeClasses.isEmpty()){
                    if(ObjectUtil.isNotEmpty(innerArgsList)){
                        result = method.invoke(null,innerArgsList.toArray());
                    }else {
                        result = method.invoke(null);
                    }
                }else{
                    int curArgsIndex = 0;
                    for (int i = 0; i < argsTypeClasses.size(); i++) {
                        Class<?> aClass = argsTypeClasses.get(i);
                        Object innerArg = null;
                        if(curArgsIndex < innerArgs.size()){
                            innerArg = innerArgs.get(curArgsIndex);
                        }

//                        解析可变函数
                        if(aClass.isArray() || Map.class.isAssignableFrom(aClass) || Collection.class.isAssignableFrom(aClass)){
                            int otherArgsNum = argsTypeClasses.size() - (i + 1);
                            int variableArgsNum = innerArgs.size() - otherArgsNum - i;
                            if(variableArgsNum <= 0){
                                // 没有传可变参数
                                innerArgsList.add(null);
                            }else{
                                if (aClass.isArray()) {
                                    // 获取数组元素的类型
                                    Class<?> componentType = aClass.getComponentType();
                                    if (ObjectUtil.isEmpty(innerArgs) || innerArgs.size() - curArgsIndex < 0) {
                                        innerArgsList.add(null);
                                        break ;
                                    }
                                    Object arguments = Array.newInstance(componentType, variableArgsNum);
                                    int j = 0;
                                    while (j+i < innerArgs.size() - otherArgsNum) {
                                        Object convert = null;
                                        innerArg = innerArgs.get(j+i);
                                        try {
                                            convert = Convert.convert(componentType,innerArg);
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }
                                        Array.set(arguments, j++, convert);
                                    }
                                    innerArgsList.add(arguments);
                                    curArgsIndex += j;
                                    continue ;
                                }
                                else if (Map.class.isAssignableFrom(aClass)) {
                                    Map<Object,Object> temp = new LinkedHashMap<>();
                                    Object innerArgKey = null;
                                    Object innerArgValue = null;
                                    Class<?> keyClazz = Object.class;
                                    Class<?> valueClazz = Object.class;
                                    TypeReference functionVariableArgsType = getFunctionVariableArgsType(methodName, aClass);
                                    if(ObjectUtil.isNotEmpty(functionVariableArgsType)){
                                        keyClazz = functionVariableArgsType.getArgGenericityType1();
                                        valueClazz = functionVariableArgsType.getArgGenericityType2();
                                    }
                                    int j = 0;
                                    for (;j+i < innerArgs.size() - otherArgsNum;j+=2) {
                                        Object convert = null;
                                        innerArgKey = null;
                                        innerArgKey = innerArgs.get(j+i);
                                        innerArgValue = null;
                                        try {
                                            innerArgKey = Convert.convert(keyClazz,innerArgKey);
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }
                                        if(j+i+1 < innerArgs.size()){
                                            innerArgValue = innerArgs.get(j+i+1);
                                            try {
                                                convert = Convert.convert(valueClazz,innerArgValue);
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                        if(ObjectUtil.isNotEmpty(innerArgKey)){
                                            temp.put(innerArgKey,convert);
                                        }
                                    }
                                    Object convert = null;
                                    try {
                                        convert = Convert.convert(aClass, temp);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    innerArgsList.add(convert);
                                    curArgsIndex += j;
                                    continue ;
                                }
                                else if (Collection.class.isAssignableFrom(aClass)) {
                                    List<Object> temp = new ArrayList<>();
                                    Class<?> valueClazz = Object.class;
                                    TypeReference functionVariableArgsType = getFunctionVariableArgsType(methodName, aClass);
                                    if(ObjectUtil.isNotEmpty(functionVariableArgsType)){
                                        valueClazz = functionVariableArgsType.getArgGenericityType1();
                                    }
                                    int j = 0;
                                    while (j+i < innerArgs.size() - otherArgsNum) {
                                        Object convert = null;
                                        innerArg = innerArgs.get(j+i);
                                        try {
                                            convert = Convert.convert(valueClazz,innerArg);
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }
                                        temp.add(convert);
                                        j++;
                                    }
                                    Object convert = null;
                                    try {
                                        convert = Convert.convert(aClass, temp);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    innerArgsList.add(convert);
                                    curArgsIndex += j;
                                    continue ;
                                }
                            }
                        }
                        else {
                            Object convert = null;
                            try {
                                convert = Convert.convert(aClass,innerArg);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            innerArgsList.add(convert);
                            curArgsIndex++;
                        }
                    }
                    result = method.invoke(null, innerArgsList.toArray());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }finally {
                CurContextProxy curContext = CurContext.getCurContext();
                if(ObjectUtil.isNotEmpty(curContext)){
                    curContext.setJsonSqlVisitor(null);
                }
                CurContext.remove();
            }
        }

        return result;
    }

    // endregion ======================== function end ===================================

    // region ======================== 字面量 start ===================================
    @Override
    public Object visitLiteralValue(SqlParser.LiteralValueContext ctx) {
        TerminalNode string = ctx.STRING();
        if (string != null) {
            String text = string.getText();
            return text.substring(1, text.length() - 1);
        }
        final SqlParser.BoolLableContext boolLableContext = ctx.boolLable();
        if (boolLableContext != null) {
            String text = boolLableContext.getText();
            return Boolean.parseBoolean(text);
        }

        SqlParser.IntValueContext intValueContext = ctx.intValue();
        if (intValueContext != null) {
            BigDecimal result;
            result = new BigDecimal(intValueContext.getText());
            if(ctx.getChildCount()> 1){
                // 前面有一个负号
                result = new BigDecimal("0").subtract(result);
            }
            return result;
        }

        SqlParser.DoubleValueContext doubleValueContext = ctx.doubleValue();
        if (doubleValueContext != null) {
            BigDecimal result;
            result = new BigDecimal(doubleValueContext.getText());
            if(ctx.getChildCount()> 1){
                // 前面有一个负号
                result = new BigDecimal("0").subtract(result);
            }
            return result;
        }

        SqlParser.NullLableContext nullLableContext = ctx.nullLable();
        if (nullLableContext != null) {
            return null;
        }
        return null;
    }

    @Override
    public Object visitBoolLable(SqlParser.BoolLableContext ctx) {
        String text = ctx.getText();
        return Boolean.parseBoolean(text);
    }

    @Override
    public Object visitColumnName(SqlParser.ColumnNameContext ctx) {
        TerminalNode id = ctx.ID();
        if(id != null){
            String columnName = id.getText();
            String jsonPath = "$." + columnName;
            return jsonPath;
        }
        SqlParser.JsonPathFunctionContext jsonpath = ctx.jsonPathFunction();
        if(jsonpath != null){
            String text = jsonpath.getChild(1).getText();
            text = text.substring(1,text.length()-1);
            return text;
        }

        return null;
    }

    // endregion ======================== 字面量 end ===================================


    //region    ====================== select start ============================

    @Override
    public Object visitSelectStatement(SqlParser.SelectStatementContext ctx) {
        SqlParser.SelectListContext selectListContext = ctx.selectList();
        SqlParser.TableNameContext tableNameContext = ctx.tableName();
        SqlParser.ExpressionContext expression = ctx.expression();
        String tableName = tableNameContext.getText();
        this.tableNameStack.push(tableName);
        boolean condition = true;
        if(ObjectUtil.isNotEmpty(expression)){
            condition = (Boolean) visitExpression(expression);
        }
        if(!condition){
            this.tableNameStack.pop();
            return null;
        }
        Object v = visitSelectList(selectListContext);
        this.tableNameStack.pop();
        return v == null ? "{}":v.toString();
    }

    @Override
    public Object visitStarLable(SqlParser.StarLableContext ctx) {
        return this.read("$");
    }



    @Override
    public Object visitSelectList(SqlParser.SelectListContext ctx) {
        List<SqlParser.SelectItemContext> selectItemContexts = ctx.selectItem();
        Configuration defaultConf = Configuration.defaultConfiguration();
        ParseContext parseContext = JsonPath.using(defaultConf);
        Map result = new LinkedHashMap();
        String genColName = "_c";
        int genColNameIndex = 0;
        for (SqlParser.SelectItemContext selectItemContext : selectItemContexts) {
            SqlParser.StarLableContext starLableContext = selectItemContext.starLable();
            SqlParser.RelationalExprContext relationalExprContext = selectItemContext.relationalExpr();
            SqlParser.CaseExprContext caseExprContext = selectItemContext.caseExpr();
            TerminalNode oneId = selectItemContext.ID();
            if(ObjectUtil.isNotEmpty(starLableContext)){
                Object v1 = visitStarLable(starLableContext);
                if(ObjectUtil.isNotEmpty(v1)){
                    Map tempResult = parseContext.parse(v1).read("$", Map.class);
                    if(ObjectUtil.isNotEmpty(tempResult)){
                        result.putAll(tempResult);
                    }
                }
            }
            String colName;
            if(ObjectUtil.isNotEmpty(relationalExprContext)){
                Object tempResult = visit(relationalExprContext);
                if(Objects.isNull(tempResult)){
                    continue;
                }
                if(ObjectUtil.isNotEmpty(oneId)){
                    colName = oneId.getText();
                }else{
                    String tempColName = relationalExprContext.getText();
                    if (colNamePattern.matcher(tempColName).find()) {
                        colName = tempColName;
                    }else{
                        colName = genColName + genColNameIndex;
                        genColNameIndex += 1;
                    }

                }
                result.put(colName,tempResult);
            }

            if(ObjectUtil.isNotEmpty(caseExprContext)){
                Object tempResult = visitCaseExpr(caseExprContext);
                if(Objects.isNull(tempResult)){
                    continue;
                }
                if(ObjectUtil.isNotEmpty(oneId)){
                    colName = oneId.getText();
                }else{
                    String tempColName = caseExprContext.getText();
                    if (colNamePattern.matcher(tempColName).find()) {
                        colName = tempColName;
                    }else{
                        colName = genColName + genColNameIndex;
                        genColNameIndex += 1;
                    }

                }
                result.put(colName,tempResult);
            }

        }
        return JsonPath.parse(result).jsonString();
    }

    @Override
    public Object visitInSubqueryExpression(SqlParser.InSubqueryExpressionContext ctx) {
        List<SqlParser.ColumnNameContext> columnNameContexts = ctx.columnName();
        SqlParser.NotLableContext notLableContext = ctx.notLable();
        SqlParser.SelectStatementContext selectStatementContext = ctx.selectStatement();
        SqlParser.AsLableContext asLable = ctx.asLable();
        List<SqlParser.LiteralValueContext> literalValueContexts = ctx.literalValue();

        int literalValueBeginIndex = 0;
        SqlParser.ColumnNameContext selectAsColumnNameContext = null;
        Object v1 = null;
        if(ObjectUtil.isEmpty(asLable) ){
            // asLable 为空，则仅有一个columnName
            if(ObjectUtil.isNotEmpty(columnNameContexts)){
                SqlParser.ColumnNameContext columnNameContext = columnNameContexts.get(0);
                Object jsonPath = visitColumnName(columnNameContext);
                v1 = read(jsonPath.toString());
            }else{
                SqlParser.LiteralValueContext literalValueContext = literalValueContexts.get(0);
                v1 = visitLiteralValue(literalValueContext);
                literalValueBeginIndex = 1;
            }

        }else {
            // asLable 为空，若仅有一个columnName，则一定是select的，否则就会有两个，其中第二个是select的
            if (columnNameContexts.size() == 1) {
                SqlParser.LiteralValueContext literalValueContext = literalValueContexts.get(0);
                v1 = visitLiteralValue(literalValueContext);
                literalValueBeginIndex = 1;
                selectAsColumnNameContext = columnNameContexts.get(0);
            }else{
                SqlParser.ColumnNameContext columnNameContext = columnNameContexts.get(0);
                Object jsonPath = visitColumnName(columnNameContext);
                v1 = read(jsonPath.toString());
                selectAsColumnNameContext = columnNameContexts.get(1);
            }

        }

        List<Object> inValueList = new ArrayList<>();
        if(ObjectUtil.isNotEmpty(literalValueContexts)){
            for (; literalValueBeginIndex < literalValueContexts.size(); literalValueBeginIndex++) {
                SqlParser.LiteralValueContext literalValueContext = literalValueContexts.get(literalValueBeginIndex);
                Object o = visitLiteralValue(literalValueContext);
                inValueList.add(o);
            }
        }

        if(ObjectUtil.isNotEmpty(selectStatementContext)){
            // asLable 为空，不会进到这里
            Object o = visitSelectStatement(selectStatementContext);
            if(ObjectUtil.isNotEmpty(o)){
                String v = o.toString();
                try {
                    Object read = JsonPath.parse(v).read(selectAsColumnNameContext.getText());
                    if(read instanceof Collection){
                        Collection tempV = (Collection)read;
                        if(ObjectUtil.isNotEmpty(tempV)){
                            inValueList.addAll(tempV);
                        }
                    }else{
                        inValueList.add(read);
                    }
                }catch (Exception ignored){

                }

            }
        }

        for (Object v : inValueList) {
            Integer i = CompareUtil.compareValue(v1, v);
            if(ObjectUtil.isNotEmpty(notLableContext)){
                if(ObjectUtil.isNull(i)){
                    return true;
                }else if(i==0){
                    return false;
                }
            }else{
                if(ObjectUtil.isNull(i)){
                    return false;
                }else if(i==0){
                    return true;
                }
            }
        }
        return ObjectUtil.isNotEmpty(notLableContext);
    }

    @Override
    public Object visitExistsSubqueryExpression(SqlParser.ExistsSubqueryExpressionContext ctx) {
        SqlParser.SelectStatementContext selectStatementContext = ctx.selectStatement();
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        SqlParser.NotLableContext notLableContext = ctx.notLable();
        Object o = visitSelectStatement(selectStatementContext);
        Object result = null;
        if(ObjectUtil.isNotEmpty(o)){
            String v = o.toString();
            try {
                Object jsonPath = visitColumnName(columnNameContext);
                result = JsonPath.parse(v).read(jsonPath.toString());
            }catch (Exception ignored){
            }
        }
        if(ObjectUtil.isNotEmpty(notLableContext)){
            return Objects.isNull(result);
        }
        return Objects.nonNull(result);
    }

    @Override
    public Object visitBetweenExpression(SqlParser.BetweenExpressionContext ctx) {
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        SqlParser.LiteralValueContext literalValueContext = ctx.literalValue();
        SqlParser.NotLableContext notLableContext = ctx.notLable();
        List<SqlParser.RelationalExprContext> relationalExprContexts = ctx.relationalExpr();
        SqlParser.RelationalExprContext relationalExprContext0 = relationalExprContexts.get(0);
        SqlParser.RelationalExprContext relationalExprContext1 = relationalExprContexts.get(1);
        Object v1 = null;
        if(ObjectUtil.isNotEmpty(columnNameContext)){
            Object jsonPath = visitColumnName(columnNameContext);
            v1 = read(jsonPath.toString());
        }else{
            v1 = visitLiteralValue(literalValueContext);
        }

        Object v2 = visit(relationalExprContext0);
        Object v3 = visit(relationalExprContext1);

        Integer flag0 = CompareUtil.compareValue(v1, v2);
        Integer flag1 = CompareUtil.compareValue(v1, v3);
        if(ObjectUtil.isNotEmpty(notLableContext)){
            if(ObjectUtil.hasNull(flag0,flag1)){
                // null和任何值比较都是false
                return true;
            }else if (flag0 < 0 || flag1 > 0) {
                return true;
            }
            return false;
        }else{
            if(ObjectUtil.hasNull(flag0,flag1)){
                return false;
            }else if (flag0 >= 0 && flag1 <= 0) {
                return true;
            }
            return false;
        }
    }

    @Override
    public Object visitLikeExpression(SqlParser.LikeExpressionContext ctx) {
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        SqlParser.LiteralValueContext literalValueContext = ctx.literalValue();
        SqlParser.NotLableContext notLableContext = ctx.notLable();
        TerminalNode stringValueContext = ctx.STRING();
        Object v1 = null;
        if(ObjectUtil.isNotEmpty(columnNameContext)){
            Object jsonPath = visitColumnName(columnNameContext);
            v1 = read(jsonPath.toString());
        }else{
            v1 = visitLiteralValue(literalValueContext);
        }
        if (Objects.isNull(v1)) {
            return false;
        }
        String text = stringValueContext.getText();
        Object reg = text.substring(1,text.length() - 1);
        Pattern valuePattern = Pattern.compile(reg.toString());
        if(ObjectUtil.isNotEmpty(notLableContext)){
            if (valuePattern.matcher(v1.toString()).find()) {
                return false;
            }
            return true;
        }else{
            if (valuePattern.matcher(v1.toString()).find()) {
                return true;
            }
            return false;
        }
    }



    //endregion ====================== select end ============================

    //region   ====================== delete start ============================

    @Override
    public Object visitDeleteStatement(SqlParser.DeleteStatementContext ctx) {
        SqlParser.TableNameContext tableNameContext = ctx.tableName();
        SqlParser.DelClauseContext delClauseContext = ctx.delClause();
        SqlParser.ExpressionContext expression = ctx.expression();
        if(ObjectUtil.isNotEmpty(tableNameContext)){
            String tableName = tableNameContext.getText();
            this.tableNameStack.push(tableName);
        }else{
            this.tableNameStack.push(MAIN_TABLE_NAME);
        }
        boolean condition = true;
        if(ObjectUtil.isNotEmpty(expression)){
            condition = (Boolean) visitExpression(expression);
        }

        if(!condition){
            this.tableNameStack.pop();
            return 0;
        }
        Integer flag = (Integer)visitDelClause(delClauseContext);
        this.tableNameStack.pop();
        return flag;
    }

    @Override
    public Object visitDelClause(SqlParser.DelClauseContext ctx) {
        List<SqlParser.ColumnNameContext> columnNameContexts = ctx.columnName();
        String tableName = this.tableNameStack.peek();
        for (SqlParser.ColumnNameContext columnNameContext : columnNameContexts) {
            Object jsonPath = visitColumnName(columnNameContext);
            delCol(tableName,jsonPath.toString());
        }
        return 1;
    }

    //endregion ====================== delete end  ============================

    //region   ====================== 宏定义 start ============================
    public Object getMacro(MacroEnum macroEnum) {
        String tableName = tableNameStack.peek();
        if(macroEnum != null){
            switch (macroEnum){
                case ORIGINAL_JSON:
                    return this.getTableContextOriginalJson(tableName);
                case READ_DOCUMENT:
                case ORIGINAL_WRITE_DOCUMENT:
                    return this.getTableContextDocument(tableName);
                case COPY_WRITE_WRITE_DOCUMENT:
                    return this.getTableContextNewDocument(tableName);
                case CUR_WRITE_DOCUMENT:
                    Boolean writeModel = this.getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
                    return writeModel ? this.getTableContextNewDocument(tableName) : this.getTableContextDocument(tableName);
                case ALL_UDF_DESC_INFO:
                    return this.getAllFunctionDescInfo();
                case ALL_TABLE_NAME:
                    return this.getAllTableName();
                case CUR_CONTEXT_PROXY:
                    CurContextProxy curContext = CurContext.getCurContext();
                    if(ObjectUtil.isNotEmpty(curContext)){
                        return curContext;
                    }
                    CurContext.set(new CurContextProxy(this));
                    return CurContext.getCurContext();
                case TABLE_NOT_OPERABLE_DATA:
                    List<Object> macroParamArgs = MacroParamArgsContext.getMacroParamArgs();
                    if(ObjectUtil.isEmpty(macroParamArgs)){
                        return null;
                    }
                    Map<String,DocumentContext> result = new HashMap<>();
                    for (Object macroParamArg : macroParamArgs) {
                        if(ObjectUtil.isEmpty(macroParamArg)){
                            continue;
                        }
                        DocumentContext tableContextCurWriteDocument = this.getTableContextCurWriteDocument(macroParamArg.toString());
                        if(ObjectUtil.isNotEmpty(tableContextCurWriteDocument)){
                            try {
                                String s = tableContextCurWriteDocument.jsonString();
                                DocumentContext parse = JsonPath.parse(s);
                                result.put(macroParamArg.toString(),parse);
                            }catch (Exception ignored){}
                        }
                    }
                    return result;
                case TABLE_OPERABLE_DATA:
                    List<Object> macroParamArgsTemp = MacroParamArgsContext.getMacroParamArgs();
                    if(ObjectUtil.isEmpty(macroParamArgsTemp)){
                        return null;
                    }
                    Map<String,DocumentContext> resultTemp = new HashMap<>();
                    for (Object macroParamArg : macroParamArgsTemp) {
                        if(ObjectUtil.isEmpty(macroParamArg)){
                            continue;
                        }
                        DocumentContext tableContextCurWriteDocument = this.getTableContextCurWriteDocument(macroParamArg.toString());
                        if(ObjectUtil.isNotEmpty(tableContextCurWriteDocument)){
                            resultTemp.put(macroParamArg.toString(),tableContextCurWriteDocument);
                        }
                    }
                    return resultTemp;
                default: return null;
            }
        }
        return null;
    }

    //endregion   ====================== 宏定义 end ============================

    @Data
    public static class WhenThenResult implements Serializable {

        private static final long serialVersionUID = 911512704921004528L;

        private Boolean conditionTrue = false;

        private Object result;
    }

}