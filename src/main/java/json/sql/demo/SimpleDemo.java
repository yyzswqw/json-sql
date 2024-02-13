package json.sql.demo;

import com.jayway.jsonpath.DocumentContext;
import json.sql.JsonSqlContext;
import json.sql.config.TableConfig;
import json.sql.enums.MacroEnum;
import json.sql.udf.ListTypeReference;
import json.sql.udf.MapTypeReference;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SimpleDemo {

    private static String jsonStr = "{\n" +
            "\t\"p3\": \"reference\",\n" +
            "\t\"p4\": 5,\n" +
            "\t\"p1\": \"aa\",\n" +
            "\t\"store\": {\n" +
            "\t\t\"book\": [{\n" +
            "\t\t\t\t\"category\": \"reference\",\n" +
            "\t\t\t\t\"author\": \"Nigel Rees\",\n" +
            "\t\t\t\t\"title\": \"Sayings of the Century\",\n" +
            "\t\t\t\t\"price\": 8.95\n" +
            "\t\t\t}, {\n" +
            "\t\t\t\t\"category\": \"fiction\",\n" +
            "\t\t\t\t\"author\": \"Evelyn Waugh\",\n" +
            "\t\t\t\t\"title\": \"Sword of Honour\",\n" +
            "\t\t\t\t\"price\": 12.99\n" +
            "\t\t\t}, {\n" +
            "\t\t\t\t\"category\": \"fiction\",\n" +
            "\t\t\t\t\"author\": \"Herman Melville\",\n" +
            "\t\t\t\t\"title\": \"Moby Dick\",\n" +
            "\t\t\t\t\"isbn\": \"0-553-21311-3\",\n" +
            "\t\t\t\t\"price\": 8.99\n" +
            "\t\t\t}, {\n" +
            "\t\t\t\t\"category\": \"fiction\",\n" +
            "\t\t\t\t\"author\": \"J. R. R. Tolkien\",\n" +
            "\t\t\t\t\"title\": \"The Lord of the Rings\",\n" +
            "\t\t\t\t\"isbn\": \"0-395-19395-8\",\n" +
            "\t\t\t\t\"price\": 22.99\n" +
            "\t\t\t}\n" +
            "\t\t],\n" +
            "\t\t\"bicycle\": {\n" +
            "\t\t\t\"color\": \"red\",\n" +
            "\t\t\t\"price\": 19.95,\n" +
            "\t\t\t\"abc\":{\"a\":1,\"b\":true,\"ac\":{\"xc\":\"qwq\",\"xv\":1234}}," +
            "\t\t\t\"abcd1\":[{\"ab1\":2,\"bb1\":false},{\"acb1\":[{\"xcb1\":\"qwq1\"},{\"xvb1\":12345}]}]" +
            "\t\t},\n" +
            "\t\t\"temp1\":[{\"tempab1\":1,\"tempbb1\":true},{\"tempacb1\":[{\"xcb1\":\"qwq\"},{\"tempxvb1\":1234}]}]," +
            "\t\t\"temp2\":\"[{\\\"tempab1\\\":1,\\\"tempbb1\\\":true},{\\\"tempab1\\\":2,\\\"tempbb1\\\":false}]\"," +
            "\t\t\"temp3\":[\"{\\\"tempab1\\\":1,\\\"tempbb1\\\":\\\"{\\\\\\\"a\\\\\\\":1}\\\"}\",{\"tempab1\":2,\"tempbb1\":false}]," +
            "\t\t\"temp4\":\"[\\\"a\\\",\\\"b\\\",2,1,\\\"c\\\",true]\"," +
            "\t\t\"temp5\":\"{\\\"abcd\\\":\\\"[\\\\\\\"a\\\\\\\",\\\\\\\"b\\\\\\\",2,1,\\\\\\\"c\\\\\\\",true]\\\"}\"" +
            "\t}\n" +
            "}";

    public static void main(String[] args) {
        JsonSqlContext jsonSqlContext = new JsonSqlContext();
        registerCustomMethod(jsonSqlContext);
//        String sql = "update a1 SET jsonPath('name') = jsonPath('$.store.book[*].author'),aa=-4-1,ab = $explode(true,'store.bicycle',true,true,false,1,'abc','abcd1'),age = jsonPath('age')%4 + age";
//        String sql = "update a1 SET jsonPath('name') = jsonPath('$.store.book[*].author'),aa=-4-1,ab = $valuesByLevel('store.bicycle',10,'xvb1'),temp2Size = $size('store.temp2'),size = $size('ab',true),age = jsonPath('age')%4 + age";
//        String sql = "update a1 SET jsonPath('name') = jsonPath('$.store.book[*].author'),aa=-4-1,ab = $format('store.temp4',2,'a'),age = jsonPath('age')%4 + age";
        String sql = "update a1 SET jsonPath('name') = jsonPath('$.store.book[*].author'),aa=-4-1,$format('$'),ab = $explode(true,'store.bicycle',true,true,false,1),age = jsonPath('age')%4 + age,$delIfNull('age') where $f(1,2,3,4,5,6) > 0 and $f1(1,2,3,4,5,6) > 0 and $f2(1,2,3,4,5,6) > 0";
//        String sql = "update a1 SET jsonPath('name') = jsonPath('$.store.book[*].author'),aa=-4-1,ab = $explode('store.temp2',10,'a1','b1',1,true),age = jsonPath('age')%4 + age";
//        String sql = "update a1 set jsonPath('name') = jsonPath('$..book[-1:][\"category\"]'),age = jsonPath('age')%4 + age,p1=123 where p2 is not null or (p3 = p1 and (p4 is null))";
//        String sql = "update a1 SET jsonPath('name') = jsonPath('$..book[-1:][\"category\"]'),age = jsonPath('age')%4 + age,p1=123 where p2 = 'aa'";
//        String sql = "update a1 SET jsonPath(\"name\") = jsonPath(\"$..book[:3]['category']\"),age = jsonPath(\"age\")%4 + age";
//        String sql = "update a1 SET age = jsonPath('$.age')%4 + age,name = 1";
//        String sql = "update a1 SET name = 'a',age = 31 where name = 'a1'";
//        String sql = "update a1 SET name = 'a',age = 31 where true";
//        String sql = "update a1 SET name = 'a',age = 31 where aa IS NOT NULL";
//        String sql = "update a1 SET name = 'a',age = 31,ab=jsonPath('$..book[?(@.isbn)]') where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31,ab=jsonPath('$..*') where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31,ab=jsonPath('$..book[?(@.price<10)]') where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31,name=false where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31.32,name=null where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31.32,name=null where 1.1 >= 1.1";
//        String sql = "update a1 SET name = 'a',age = 1,a1 = " +
//                "case when age = 1 then 'a' " +
//                "when age < 10 then true " +
//                "when age <20 and name = 'a' then 5.9 " +
//                "when dd is not null then null " +
//                "else toJson('{\"a\":23,\"b\":\"ab\"}') end " +
//                "else toJsonByPath('$..book[:3][\"category\"]') end " +
//                " where true = true";
//        String sql = "update a1 SET $del('$.store.book[0].category','$.store.book[1].category','store.bicycle.abc.$a'),age = 31.32 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1 and 22.99  in (1,2,3,22.99)";
//        String sql = "update a1 SET $del('$.store.book[0].category'),age = 31.32 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1 and 22.99  in (select jsonPath('$.store.book[*].price') as abc from b1 where 1=1 as abc)";
//        String sql = "update a1 SET $del('$.store.book[0].category'),age = 31.32 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1";
//        String sql = "select *,jsonPath('$.store.book[0].category') from a1 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1";
//        String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where 22.99 in (select 1 as abc from b1 where 1=1 as abc)";
//        String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where 22.99 not in (1,2,3,22.99)";
//        String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where not exists (select 1 from b1 where 1!=1 as _c0)";
//        String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where 3 between 1+1 and p4*2";
//        String sql = "select case when 1>2 then 1 end as c,1 as b ,jsonPath('$..book[0][\"category\"]') as c from a1 where p3 like '^refer'";
//        String sql = "select case when 1>2 then 1 end as c from a1 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1";
//        String sql = "select p1,p2,p3,p4 + 1 as aA1,p5,toJson('{\"a\":1}') from b1";
//        String sql = "delete a1 p1,p2,p3,jsonPath('$..book[0][\"category\"]') where p4 != 5";
//        String sql = "delete a1 p1,p2,p3,jsonPath('$..book[0][\"category\"]') where 51 in (select p4 from b1 as p4)";
//        String sql = "select * from a1";
        jsonSqlContext.registerTable("a1", jsonStr);
        jsonSqlContext.registerTable("b1", jsonStr);
        jsonSqlContext.setTableConfig("a1", TableConfig.WRITE_MODEL,true);
        String exec = jsonSqlContext.sql(sql);
        System.out.println("最终结果:"+exec);
        System.out.println();
        System.out.println("最终结果 table a1 :"+jsonSqlContext.getTable("a1"));
        System.out.println();
        System.out.println("最终结果 table b1 :"+jsonSqlContext.getTable("b1"));
        System.out.println("查询指定结果 :"+jsonSqlContext.readCurWriteDocument("a1", "$"));
    }

    private static void registerCustomMethod( JsonSqlContext jsonSqlContext) {
        try {
            Method a = UdfDemo.class.getMethod("a", Number.class, Object.class);
            Method b = UdfDemo.class.getMethod("b", BigDecimal.class, BigDecimal.class);
            Method c = UdfDemo.class.getMethod("c", Object.class,Object.class,Object.class,Object.class,Object.class,BigDecimal.class, BigDecimal.class);
            Method d = UdfDemo.class.getMethod("d", Object.class,Object.class,Object.class,Object.class, DocumentContext.class);

            jsonSqlContext.registerFunction("a", a,Number.class, Object.class);
            jsonSqlContext.registerFunction("b", b,BigDecimal.class, BigDecimal.class);
            jsonSqlContext.registerFunction("c", c,BigDecimal.class, BigDecimal.class);
            jsonSqlContext.registerFunction("d", d);

            jsonSqlContext.registerMacro("c", MacroEnum.ORIGINAL_JSON,MacroEnum.READ_DOCUMENT,MacroEnum.ORIGINAL_WRITE_DOCUMENT,MacroEnum.COPY_WRITE_WRITE_DOCUMENT,MacroEnum.CUR_WRITE_DOCUMENT);
            jsonSqlContext.registerMacro("d", MacroEnum.ORIGINAL_JSON,MacroEnum.READ_DOCUMENT,MacroEnum.ORIGINAL_WRITE_DOCUMENT,MacroEnum.COPY_WRITE_WRITE_DOCUMENT,MacroEnum.CUR_WRITE_DOCUMENT);

            Method e = UdfDemo.class.getMethod("e", Object.class,Object.class,String[].class);
            Method e1 = UdfDemo.class.getMethod("e1", Object.class,String[].class,Object.class);
            Method e2 = UdfDemo.class.getMethod("e2", String[].class,Object.class,Object.class);
            jsonSqlContext.registerFunction("e", e,Object.class, Object.class,String[].class);
            jsonSqlContext.registerFunction("e1", e1,Object.class,String[].class,Object.class);
            jsonSqlContext.registerFunction("e2", e2,String[].class,Object.class,Object.class);

            Method f = UdfDemo.class.getMethod("f", Object.class,Object.class, List.class);
            Method f1 = UdfDemo.class.getMethod("f1", Object.class,List.class,Object.class);
            Method f2 = UdfDemo.class.getMethod("f2", List.class,Object.class,Object.class);
            jsonSqlContext.registerFunction("f", f,Object.class,Object.class, List.class);
            jsonSqlContext.registerFunction("f1", f1,Object.class,List.class,Object.class);
            jsonSqlContext.registerFunction("f2", f2,List.class,Object.class,Object.class);
            jsonSqlContext.registerFunctionVariableArgsType("f",List.class,new ListTypeReference(String.class));
            jsonSqlContext.registerFunctionVariableArgsType("f1",List.class,new ListTypeReference(String.class));
            jsonSqlContext.registerFunctionVariableArgsType("f2",List.class,new ListTypeReference(String.class));

            Method g = UdfDemo.class.getMethod("g", Object.class,Object.class, Map.class);
            Method g1 = UdfDemo.class.getMethod("g1", Object.class,Map.class,Object.class);
            Method g2 = UdfDemo.class.getMethod("g2", Map.class,Object.class,Object.class);
            jsonSqlContext.registerFunction("g", g,Object.class,Object.class, Map.class);
            jsonSqlContext.registerFunction("g1", g1,Object.class,Map.class,Object.class);
            jsonSqlContext.registerFunction("g2", g2,Map.class,Object.class,Object.class);
            jsonSqlContext.registerFunctionVariableArgsType("g", Map.class, new MapTypeReference(String.class,String.class));
            jsonSqlContext.registerFunctionVariableArgsType("g1", Map.class, new MapTypeReference(String.class,String.class));
            jsonSqlContext.registerFunctionVariableArgsType("g2", Map.class, new MapTypeReference(String.class,String.class));


        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


}
