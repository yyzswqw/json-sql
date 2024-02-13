package json.sql;

import cn.hutool.core.util.ObjectUtil;
import json.sql.annotation.PackageAnnotationScanner;
import json.sql.annotation.UdfClass;
import json.sql.annotation.UdfMethod;
import json.sql.annotation.UdfParser;
import json.sql.enums.MacroEnum;
import json.sql.grammar.JsonSqlVisitor;
import json.sql.grammar.ParserErrorListener;
import json.sql.udf.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class JsonSqlContext {

    private final JsonSqlVisitor jsonSqlVisitor;

    public JsonSqlContext(){
        this.jsonSqlVisitor = new JsonSqlVisitor();
        Set<Method> udfMethods = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(UdfMethod.class);
        if(ObjectUtil.isNotEmpty(udfMethods)){
            for (Method udfMethod : udfMethods) {
                try {
                    UdfParser.registerUdfMethod(this,udfMethod);
                }catch (Exception e){
                    Class<?>[] parameterTypes = udfMethod.getParameterTypes();
                    // 获取所在类的 Class 对象
                    Class<?> clazz = udfMethod.getDeclaringClass();
                    log.info("注册udf 函数失败!class : {} ,method : {} ,parameterTypes : {}",clazz.getName(),udfMethod.getName(),parameterTypes);
                    log.error("注册udf 函数失败!",e);
                }
            }
        }
        Set<Class<?>> classes = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(UdfClass.class);
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                UdfParser.classParser(this,aClass, false, (Method[]) null);
            }
        }
    }

    /**
     * 删除表
     * @param tableName 表名
     */
    public void dropTable(String tableName) {
        jsonSqlVisitor.dropTable(tableName);
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
        return jsonSqlVisitor.getUdfFunction(functionName);
    }

    /**
     * 获取所有 udf 函数参数列表
     * @return 函数及其参数列表
     */
    public Map<String,List<Class<?>>> getAllUdfFunction(){
        return jsonSqlVisitor.getAllUdfFunction();
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
