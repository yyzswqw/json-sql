package json.sql;

import json.sql.enums.MacroEnum;
import json.sql.grammar.JsonSqlVisitor;

import java.util.Map;

public class CurContextProxy {

    private JsonSqlVisitor jsonSqlVisitor;

    public CurContextProxy(JsonSqlVisitor jsonSqlVisitor){
        this.jsonSqlVisitor = jsonSqlVisitor;
    }

    public void setJsonSqlVisitor(JsonSqlVisitor jsonSqlVisitor){
        this.jsonSqlVisitor = jsonSqlVisitor;
    }

    public <T> T getMacro(MacroEnum macroEnum) {
        return (T) jsonSqlVisitor.getMacro(macroEnum);
    }

    public void registerTable(String tableName, String json, Map<String,Object> config) {
        jsonSqlVisitor.registerTable(tableName,json,config);
    }

    public String sql(String sql) {
        return jsonSqlVisitor.sql(sql);
    }

    public String getTableResult(String tableName) {
        return jsonSqlVisitor.getResult(tableName);
    }

    public String jsonToCsv(String json,boolean outputHeader) {
        return jsonSqlVisitor.jsonToCsv(json,outputHeader);
    }

}
