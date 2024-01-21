package json.sql;

import json.sql.enums.MacroEnum;
import json.sql.grammar.JsonSqlVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.reflect.Method;
import java.util.Map;

public class JsonSqlContext {

    public static void registerTable(String tableName,String json) {
        JsonSqlVisitor.registerTable(tableName,json);
    }

    public static void registerTable(String tableName, String json, Map<String,Object> config) {
        JsonSqlVisitor.registerTable(tableName,json,config);
    }

    public static void setTableConfig(String tableName,String key,Object value) {
        JsonSqlVisitor.setTableConfig(tableName, key,value);
    }

    public static void registerFunction(String functionName, Method method, Class<?> ... argsType){
        JsonSqlVisitor.registerFunction(functionName,method,argsType);
    }

    public static void registerMacro(String functionName, MacroEnum... macros) {
        JsonSqlVisitor.registerMacro(functionName,macros);
    }

    public static String sql(String sql){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.sql();
        return JsonSqlVisitor.exec(tree);
    }

    public static String select(String sql){
        return sql(sql);
    }

    public static String update(String sql){
        return sql(sql);
    }

    public static String delete(String sql){
        return sql(sql);
    }

    public static String sql(String sql,String jsonString){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.sql();
        return JsonSqlVisitor.exec(tree,null,jsonString);
    }

    public static String sql(String sql,Boolean writeModel){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.sql();
        return JsonSqlVisitor.exec(tree,writeModel,null);
    }

    public static String sql(String sql,Boolean writeModel,String jsonString){
        return JsonSqlVisitor.exec(sql,writeModel,jsonString);
    }

    public static String getTable(String tableName){
        return JsonSqlVisitor.getResult(tableName);
    }

    public static <T> T getTableConfig(String tableName,String key,Class<T> classType){
        return JsonSqlVisitor.getTableContextConfig( tableName, key,classType);
    }

    public static <T> T getTableContextConfig(String tableName,String key,Class<T> classType,T defaultValue){
        return JsonSqlVisitor.getTableContextConfig( tableName, key,classType,defaultValue);
    }



}
