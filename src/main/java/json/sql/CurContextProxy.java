package json.sql;

import json.sql.enums.MacroEnum;
import json.sql.grammar.JsonSqlVisitor;

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

}
