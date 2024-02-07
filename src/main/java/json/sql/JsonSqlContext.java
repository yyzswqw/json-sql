package json.sql;

import json.sql.enums.MacroEnum;
import json.sql.grammar.JsonSqlVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.reflect.Method;
import java.util.Map;

public class JsonSqlContext {

    private final JsonSqlVisitor jsonSqlVisitor = new JsonSqlVisitor();

    public void registerTable(String tableName,String json) {
        jsonSqlVisitor.registerTable(tableName,json);
    }

    public void registerTable(String tableName, String json, Map<String,Object> config) {
        jsonSqlVisitor.registerTable(tableName,json,config);
    }

    public void setTableConfig(String tableName,String key,Object value) {
        jsonSqlVisitor.setTableConfig(tableName, key,value);
    }

    /**
     * argsType不需要定义宏参数，宏参数使用registerMacro(String functionName, MacroEnum... macros);
     * @param functionName 方法名
     * @param method 方法逻辑method
     * @param argsType 参数类型
     */
    public void registerFunction(String functionName, Method method, Class<?> ... argsType){
        jsonSqlVisitor.registerFunction(functionName,method,argsType);
    }

    public void registerMacro(String functionName, MacroEnum... macros) {
        jsonSqlVisitor.registerMacro(functionName,macros);
    }

    public String sql(String sql){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.sql();
        return jsonSqlVisitor.exec(tree);
    }

    public String select(String sql){
        return sql(sql);
    }

    public String update(String sql){
        return sql(sql);
    }

    public String delete(String sql){
        return sql(sql);
    }

    public String sql(String sql,String jsonString){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.sql();
        return jsonSqlVisitor.exec(tree,null,jsonString);
    }

    public String sql(String sql,Boolean writeModel){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.sql();
        return jsonSqlVisitor.exec(tree,writeModel,null);
    }

    public String sql(String sql,Boolean writeModel,String jsonString){
        return jsonSqlVisitor.exec(sql,writeModel,jsonString);
    }

    public String getTable(String tableName){
        return jsonSqlVisitor.getResult(tableName);
    }

    public <T> T getTableConfig(String tableName,String key,Class<T> classType){
        return jsonSqlVisitor.getTableContextConfig( tableName, key,classType);
    }

    public <T> T getTableContextConfig(String tableName,String key,Class<T> classType,T defaultValue){
        return jsonSqlVisitor.getTableContextConfig( tableName, key,classType,defaultValue);
    }



}
