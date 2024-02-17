package json.sql;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import json.sql.entity.UdfFunctionDescInfo;
import json.sql.enums.MacroEnum;
import json.sql.grammar.*;
import json.sql.lister.LifecycleListener;
import json.sql.udf.CustomMethodFactory;
import json.sql.udf.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class JsonSqlContext {

    private final JsonSqlVisitor jsonSqlVisitor;

    private final List<LifecycleListener> lifecycleListeners = new ArrayList<>();

    private JsonSqlContext(){
        this.jsonSqlVisitor = new JsonSqlVisitor();
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {

        private final List<LifecycleListener> lifecycleListeners = new ArrayList<>();

        public void addLifecycleListener(LifecycleListener listener){
            lifecycleListeners.add(listener);
        }

        public JsonSqlContext build(){
            JsonSqlContext jsonSqlContext = new JsonSqlContext();
            if(ObjectUtil.isNotEmpty(lifecycleListeners)){
                jsonSqlContext.getLifecycleListener().addAll(lifecycleListeners);
            }
            CustomMethodFactory.registerCustomMethod(jsonSqlContext);
            return jsonSqlContext;
        }

    }

    public List<LifecycleListener> getLifecycleListener(){
        return this.lifecycleListeners;
    }

    /**
     * 判断数据是否是json格式
     * @param data json string
     * @return true:是，false:否
     */
    public boolean isJsonData(String data){
        return this.jsonSqlVisitor.isJsonData(data);
    }

    /**
     * 判断表中当前数据是否是json格式
     * @param tableName 表名
     * @return true:是，false:否
     */
    public boolean isJson(String tableName){
        return this.jsonSqlVisitor.isJson(tableName);
    }


    /**
     * 检查sql语法是否正确
     * @param sql sql
     * @return true:是，false:否
     */
    public boolean isSql(String sql){
        return this.jsonSqlVisitor.isSql(sql);
    }

    /**
     * 判断是否存在表
     * @param tableName 表名
     * @return true:存在，false:不存在
     */
    public boolean hasTable(String tableName){
        return this.jsonSqlVisitor.hasTable(tableName);
    }

    /**
     * 获取sql语法错误信息
     * @param sql sql
     * @return 如果有语法错误，返回错误信息，没有则为空
     */
    public List<String> getSqlError(String sql){
        return this.jsonSqlVisitor.getSqlError(sql);
    }

    /**
     * 删除表
     * @param tableName 表名
     */
    public Integer dropTable(String tableName) {
        return jsonSqlVisitor.dropTable(tableName);
    }

    /**
     * 注册表
     * @param tableName 表名
     * @param json json值
     */
    public void registerTable(String tableName,String json) {
        jsonSqlVisitor.registerTable(tableName,json);
    }

    /**
     * 注册表
     * @param tableName 表名
     * @param json json值
     * @param config 配置项
     */
    public void registerTable(String tableName, String json, Map<String,Object> config) {
        jsonSqlVisitor.registerTable(tableName,json,config);
    }

    /**
     * 设置表的配置项
     * @param tableName 表名
     * @param key 配置项key
     * @param value 值
     */
    public void setTableConfig(String tableName,String key,Object value) {
        jsonSqlVisitor.setTableConfig(tableName, key,value);
    }

    /**
     * 注册udf函数描述信息
     * @param functionName 函数名
     * @param functionDescInfo 描述信息
     */
    public void registerFunctionDescInfo(String functionName, UdfFunctionDescInfo functionDescInfo){
        jsonSqlVisitor.registerFunctionDescInfo(functionName,functionDescInfo);
    }

    /**
     * 移除一个UDF函数
     * @param udfName udf名称
     */
    public void removeUdf(String udfName){
        jsonSqlVisitor.removeUdf(udfName);
    }

    /**
     * 获取udf函数描述信息
     * @param functionName 函数名
     * @return udf函数描述信息
     */
    public UdfFunctionDescInfo getFunctionDescInfo(String functionName){
        return jsonSqlVisitor.getFunctionDescInfo(functionName);
    }

    /**
     * 获取所有udf函数描述信息
     * @return udf函数描述信息
     */
    public Collection<UdfFunctionDescInfo> getAllFunctionDescInfo(){
        Collection<UdfFunctionDescInfo> allFunctionDescInfo = jsonSqlVisitor.getAllFunctionDescInfo();
        if(ObjectUtil.isNotEmpty(allFunctionDescInfo)){
            return Collections.unmodifiableCollection(allFunctionDescInfo);
        }
        return null;
    }

    /**
     * 获取所有注册的表名
     * @return  所有注册的表名
     */
    public Set<String> getAllTableName(){
        return jsonSqlVisitor.getAllTableName();
    }

    /**
     * 打印一个 udf 函数描述信息
     */
    public void showUdfDesc(String functionName){
        String desc = jsonSqlVisitor.showUdfDesc(functionName);
        Console.log(desc);
    }

    /**
     * 打印所有udf 函数描述信息
     */
    public void showUdfDesc(){
        String desc = jsonSqlVisitor.showUdfDesc();
        Console.log(desc);
    }

    /**
     * 注册自定义UDF函数
     * @param functionName 函数名
     * @param method 实现的具体方法，只能是静态方法
     * @param macros 宏参数列表
     * @param argsType 参数列表，不包含宏参数
     */
    public void registerFunction(String functionName,Method method,MacroEnum[] macros,Class<?>[] argsType){
        jsonSqlVisitor.registerFunction(functionName,method,macros,argsType);
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
        jsonSqlVisitor.registerFunction(functionName,method,macros,argsType,variableArgsType,genericityArgsType);
    }

    /**
     * 注册自定义UDF函数
     * @param functionName 函数名
     * @param method 方法逻辑method,只能是静态方法
     * @param argsType 参数类型列表,不需要定义宏参数，宏参数使用registerMacro(String functionName, MacroEnum... macros) 注册;
     */
    public void registerFunction(String functionName, Method method, Class<?> ... argsType){
        jsonSqlVisitor.registerFunction(functionName,method,argsType);
    }

    /**
     * 注册函数中可变参数的泛型类型
     * @param functionName 函数名
     * @param argsType 外层参数类型
     * @param genericityArgsType 泛型类型
     */
    public void registerFunctionVariableArgsType(String functionName, Class<?> argsType, TypeReference genericityArgsType){
        jsonSqlVisitor.registerFunctionVariableArgsType(functionName,argsType,genericityArgsType);
    }

    /**
     * 获取函数中可变参数的泛型类型
     * @param functionName 函数名
     * @param argsType 外层参数类型
     * @return 泛型类型
     */
    public TypeReference getFunctionVariableArgsType(String functionName, Class<?> argsType){
        return jsonSqlVisitor.getFunctionVariableArgsType(functionName,argsType);
    }

    /**
     * 注册函数宏参数，宏参数必须定义在函数的最开始的参数
     * @param functionName 函数名
     * @param macros 参数宏列表
     */
    public void registerMacro(String functionName, MacroEnum... macros) {
        jsonSqlVisitor.registerMacro(functionName,macros);
    }

    /**
     * 获取 udf 函数参数列表
     * @param functionName 函数名
     * @return 参数列表
     */
    public Map<String,List<Class<?>>> getUdfFunction(String functionName){
        Map<String, List<Class<?>>> udfFunction = jsonSqlVisitor.getUdfFunction(functionName);
        if(ObjectUtil.isNotEmpty(udfFunction)){
            return Collections.unmodifiableMap(udfFunction);
        }
        return null;

    }

    /**
     * 获取所有 udf 函数参数列表
     * @return 函数及其参数列表
     */
    public Map<String,List<Class<?>>> getAllUdfFunction(){
        Map<String, List<Class<?>>> allUdfFunction = jsonSqlVisitor.getAllUdfFunction();
        if(ObjectUtil.isNotEmpty(allUdfFunction)){
            return Collections.unmodifiableMap(allUdfFunction);
        }
        return null;
    }

    /**
     * 执行sql
     * @param sql sql
     * @return 执行结果，为空则返回主表结果
     */
    public String sql(String sql){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);
        lexer.addErrorListener(parserErrorListener);

        // 删除默认的控制台打印的错误信息，使用自定义的错误监听器
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        int consoleErrorListenerIndex;
        do {
            consoleErrorListenerIndex = -1;
            for (int i = 0; i < errorListeners.size(); i++) {
                ANTLRErrorListener next = errorListeners.get(i);
                if(next instanceof ConsoleErrorListener){
                    consoleErrorListenerIndex = i;
                    break;
                }
            }
            if(consoleErrorListenerIndex != -1){
                errorListeners.remove(consoleErrorListenerIndex);
            }
        }while (consoleErrorListenerIndex != -1);

        List<? extends ANTLRErrorListener> lexerErrorListeners = lexer.getErrorListeners();
        do {
            consoleErrorListenerIndex = -1;
            for (int i = 0; i < lexerErrorListeners.size(); i++) {
                ANTLRErrorListener next = lexerErrorListeners.get(i);
                if(next instanceof ConsoleErrorListener){
                    consoleErrorListenerIndex = i;
                    break;
                }
            }
            if(consoleErrorListenerIndex != -1){
                lexerErrorListeners.remove(consoleErrorListenerIndex);
            }
        }while (consoleErrorListenerIndex != -1);

        ParseTree tree = parser.sql();
        if (parserErrorListener.hasError()) {
            List<String> errors = parserErrorListener.errors();
            String join = String.join("\n", errors);
            throw new RuntimeException("parser errors : "+join);
        }
        return jsonSqlVisitor.exec(tree);
    }



    /**
     * 执行 select sql
     * @param sql sql
     * @return 执行结果，为空则返回主表结果
     */
    public String select(String sql){
        return sql(sql);
    }

    /**
     * 执行 update sql
     * @param sql sql
     * @return 执行结果，为空则返回主表结果
     */
    public String update(String sql){
        return sql(sql);
    }

    /**
     * 执行 delete sql
     * @param sql sql
     * @return 执行结果，为空则返回主表结果
     */
    public String delete(String sql){
        return sql(sql);
    }

    /**
     * 执行 sql
     * @param sql sql
     * @param jsonString json值
     * @return 执行结果，为空则返回主表结果
     */
    public String sql(String sql,String jsonString){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);
        lexer.addErrorListener(parserErrorListener);
        // 删除默认的控制台打印的错误信息，使用自定义的错误监听器
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        int consoleErrorListenerIndex;
        do {
            consoleErrorListenerIndex = -1;
            for (int i = 0; i < errorListeners.size(); i++) {
                ANTLRErrorListener next = errorListeners.get(i);
                if(next instanceof ConsoleErrorListener){
                    consoleErrorListenerIndex = i;
                    break;
                }
            }
            if(consoleErrorListenerIndex != -1){
                errorListeners.remove(consoleErrorListenerIndex);
            }
        }while (consoleErrorListenerIndex != -1);

        List<? extends ANTLRErrorListener> lexerErrorListeners = lexer.getErrorListeners();
        do {
            consoleErrorListenerIndex = -1;
            for (int i = 0; i < lexerErrorListeners.size(); i++) {
                ANTLRErrorListener next = lexerErrorListeners.get(i);
                if(next instanceof ConsoleErrorListener){
                    consoleErrorListenerIndex = i;
                    break;
                }
            }
            if(consoleErrorListenerIndex != -1){
                lexerErrorListeners.remove(consoleErrorListenerIndex);
            }
        }while (consoleErrorListenerIndex != -1);

        ParseTree tree = parser.sql();
        if (parserErrorListener.hasError()) {
            List<String> errors = parserErrorListener.errors();
            String join = String.join("\n", errors);
            throw new RuntimeException("parser errors : "+join);
        }
        return jsonSqlVisitor.exec(tree,null,jsonString);
    }

    /**
     * 执行 sql
     * @param sql sql
     * @param writeModel 写模式
     * @return 执行结果，为空则返回主表结果
     */
    public String sql(String sql,Boolean writeModel){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParserErrorListener parserErrorListener = new ParserErrorListener();
        parser.addErrorListener(parserErrorListener);
        lexer.addErrorListener(parserErrorListener);
        // 删除默认的控制台打印的错误信息，使用自定义的错误监听器
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        int consoleErrorListenerIndex;
        do {
            consoleErrorListenerIndex = -1;
            for (int i = 0; i < errorListeners.size(); i++) {
                ANTLRErrorListener next = errorListeners.get(i);
                if(next instanceof ConsoleErrorListener){
                    consoleErrorListenerIndex = i;
                    break;
                }
            }
            if(consoleErrorListenerIndex != -1){
                errorListeners.remove(consoleErrorListenerIndex);
            }
        }while (consoleErrorListenerIndex != -1);

        List<? extends ANTLRErrorListener> lexerErrorListeners = lexer.getErrorListeners();
        do {
            consoleErrorListenerIndex = -1;
            for (int i = 0; i < lexerErrorListeners.size(); i++) {
                ANTLRErrorListener next = lexerErrorListeners.get(i);
                if(next instanceof ConsoleErrorListener){
                    consoleErrorListenerIndex = i;
                    break;
                }
            }
            if(consoleErrorListenerIndex != -1){
                lexerErrorListeners.remove(consoleErrorListenerIndex);
            }
        }while (consoleErrorListenerIndex != -1);

        ParseTree tree = parser.sql();
        if (parserErrorListener.hasError()) {
            List<String> errors = parserErrorListener.errors();
            String join = String.join("\n", errors);
            throw new RuntimeException("parser errors : "+join);
        }
        return jsonSqlVisitor.exec(tree,writeModel,null);
    }

    /**
     * 执行 sql
     * @param sql sql
     * @param writeModel 写模式
     * @param jsonString json值
     * @return 执行结果，为空则返回主表结果
     */
    public String sql(String sql,Boolean writeModel,String jsonString){
        return jsonSqlVisitor.exec(sql,writeModel,jsonString);
    }

    /**
     * 根据表名和jsonPath 获取值,通过当前操作的JsonDocument
     * @param tableName 表名
     * @param jsonPath jsonPath
     * @return 值
     */
    public Object readCurWriteDocument(String tableName,String jsonPath){
        return jsonSqlVisitor.readCurWriteDocument(tableName,jsonPath);
    }

    /**
     * 根据表名和jsonPath 获取值,通过原始的JsonDocument
     * @param tableName 表名
     * @param jsonPath jsonPath
     * @return 值
     */
    public Object readOriginalDocument(String tableName,String jsonPath){
        return jsonSqlVisitor.readOriginalDocument(tableName,jsonPath);
    }

    /**
     * 根据表名和jsonPath 获取值,通过 copy on write 的JsonDocument
     * @param tableName 表名
     * @param jsonPath jsonPath
     * @return 值
     */
    public Object readCopyOnWriteDocument(String tableName,String jsonPath){
        return jsonSqlVisitor.readCopyOnWriteDocument(tableName,jsonPath);
    }

    /**
     * 获取表的最终json值结果
     * @param tableName 表名
     * @return 表的json值
     */
    public String getTable(String tableName){
        return jsonSqlVisitor.getResult(tableName);
    }

    /**
     * 获取表的配置项
     * @param tableName 表名
     * @param key 配置项key
     * @param classType 转换的类型
     * @return 配置项的值
     * @param <T> 转换后返回的类型
     */
    public <T> T getTableConfig(String tableName,String key,Class<T> classType){
        return jsonSqlVisitor.getTableContextConfig( tableName, key,classType);
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
        return jsonSqlVisitor.getTableContextConfig( tableName, key,classType,defaultValue);
    }



}
