package json.sql.udf;

import com.jayway.jsonpath.DocumentContext;
import json.sql.JsonSqlContext;
import json.sql.enums.MacroEnum;

import java.lang.reflect.Method;

public class CustomMethodFactory {

    public static void registerCustomMethod(JsonSqlContext jsonSqlContext) {
        try {

            Method formatAllLevel = InnerUdfMethod.class.getMethod("formatAllLevel",DocumentContext.class,String.class,String[].class);
            jsonSqlContext.registerFunction("formatAllLevel", formatAllLevel,String.class,String[].class);
            jsonSqlContext.registerMacro("formatAllLevel",MacroEnum.CUR_WRITE_DOCUMENT);

            Method format = InnerUdfMethod.class.getMethod("format",DocumentContext.class,String.class,Long.class,String[].class);
            jsonSqlContext.registerFunction("format", format,String.class,Long.class,String[].class);
            jsonSqlContext.registerMacro("format",MacroEnum.CUR_WRITE_DOCUMENT);

            Method size = InnerUdfMethod.class.getMethod("size",DocumentContext.class,String.class,Boolean.class);
            jsonSqlContext.registerFunction("size", size,String.class,Boolean.class);
            jsonSqlContext.registerMacro("size",MacroEnum.CUR_WRITE_DOCUMENT);

            Method valuesByLevel = InnerUdfMethod.class.getMethod("valuesByLevel",DocumentContext.class,String.class,Long.class,String[].class);
            jsonSqlContext.registerFunction("valuesByLevel", valuesByLevel,String.class,Long.class,String[].class);
            jsonSqlContext.registerMacro("valuesByLevel",MacroEnum.CUR_WRITE_DOCUMENT);

            Method values = InnerUdfMethod.class.getMethod("values",DocumentContext.class,String.class,String[].class);
            jsonSqlContext.registerFunction("values", values,String.class,String[].class);
            jsonSqlContext.registerMacro("values",MacroEnum.CUR_WRITE_DOCUMENT);

            Method keysByLevel = InnerUdfMethod.class.getMethod("keysByLevel",DocumentContext.class,String.class,Long.class);
            jsonSqlContext.registerFunction("keysByLevel", keysByLevel,String.class,Long.class);
            jsonSqlContext.registerMacro("keysByLevel",MacroEnum.CUR_WRITE_DOCUMENT);

            Method keys = InnerUdfMethod.class.getMethod("keys",DocumentContext.class,String.class);
            jsonSqlContext.registerFunction("keys", keys,String.class);
            jsonSqlContext.registerMacro("keys",MacroEnum.CUR_WRITE_DOCUMENT);

            Method explode2 = InnerUdfMethod.class.getMethod("explode2", DocumentContext.class,String.class,Long.class, String[].class);
            jsonSqlContext.registerFunction("explode2", explode2,String.class,Long.class, String[].class);
            jsonSqlContext.registerMacro("explode2",MacroEnum.CUR_WRITE_DOCUMENT);

            Method explode3 = InnerUdfMethod.class.getMethod("explode3", DocumentContext.class,String.class, String[].class);
            jsonSqlContext.registerFunction("explode3", explode3,String.class, String[].class);
            jsonSqlContext.registerMacro("explode3",MacroEnum.CUR_WRITE_DOCUMENT);

            Method explode = InnerUdfMethod.class.getMethod("explode", DocumentContext.class,Boolean.class,String.class,Boolean.class,Boolean.class,Boolean.class, Long.class,String[].class);
            jsonSqlContext.registerFunction("explode", explode,Boolean.class,String.class,Boolean.class,Boolean.class,Boolean.class, Long.class,String[].class);
            jsonSqlContext.registerMacro("explode",MacroEnum.CUR_WRITE_DOCUMENT);

            Method explodeAllLevel2 = InnerUdfMethod.class.getMethod("explodeAllLevel2",DocumentContext.class,Boolean.class,String.class,Boolean.class,Boolean.class,Boolean.class, String[].class);
            jsonSqlContext.registerFunction("explodeAllLevel2", explodeAllLevel2,Boolean.class,String.class,Boolean.class,Boolean.class,Boolean.class, String[].class);
            jsonSqlContext.registerMacro("explodeAllLevel2",MacroEnum.CUR_WRITE_DOCUMENT);

            Method explodeAllLevel = InnerUdfMethod.class.getMethod("explodeAllLevel",DocumentContext.class,String.class,Boolean.class,String.class,Boolean.class,Boolean.class,Boolean.class, String[].class);
            jsonSqlContext.registerFunction("explodeAllLevel", explodeAllLevel,String.class,Boolean.class,String.class,Boolean.class,Boolean.class,Boolean.class, String[].class);
            jsonSqlContext.registerMacro("explodeAllLevel",MacroEnum.CUR_WRITE_DOCUMENT);

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
