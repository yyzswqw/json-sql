package json.sql.grammar;

import cn.hutool.core.convert.Convert;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import json.sql.parse.SqlBaseVisitor;
import json.sql.parse.SqlParser;
import json.sql.udf.CustomMethod;
import lombok.Data;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
public class JsonSqlVisitor extends SqlBaseVisitor<Object> {

    private static Table<String,Method,List<Class<?>>> methodTable = HashBasedTable.create();

    static{
        try {
            Method a = CustomMethod.class.getMethod("a", Number.class, Object.class);
            Method b = CustomMethod.class.getMethod("b", BigDecimal.class, BigDecimal.class);
            registerFunction("a", a,Number.class, Object.class);
            registerFunction("b", b,BigDecimal.class, BigDecimal.class);
            registerFunction("c", b);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerFunction(String functionName,Method method,Class<?> ... argsType){
        if(argsType == null || argsType.length == 0){
            methodTable.put(functionName,method,new ArrayList<>());
        }else{
            methodTable.put(functionName,method,Arrays.asList(argsType));
        }
    }

    private static String jsonStr = "{\n" +
            "\t\"p3\": \"aa\",\n" +
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
            "\t\t\t\"price\": 19.95\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    public static void main(String[] args) {
//        String sql = "update a1 SET jsonPath('name') = jsonPath('$.store.book[*].author'),age = jsonPath('age')%4 + age";
//        String sql = "set jsonPath('name') = jsonPath('$..book[-1:][\"category\"]'),age = jsonPath('age')%4 + age,p1=123 where p2 is not null or (p3 = p1 and (p4 is null))";
//        String sql = "update a1 SET jsonPath('name') = jsonPath('$..book[-1:][\"category\"]'),age = jsonPath('age')%4 + age,p1=123 where p2 = 'aa'";
//        String sql = "update a1 SET jsonPath(\"name\") = jsonPath(\"$..book[:3]['category']\"),age = jsonPath(\"age\")%4 + age";
//        String sql = "update a1 SET age = jsonPath('$.age')%4 + age,name = 1";
//        String sql = "update a1 SET name = 'a',age = 31 where name = 'a1'";
//        String sql = "update a1 SET name = 'a',age = 31 where true";
//        String sql = "update a1 SET name = 'a',age = 31 where aa IS NOT NULL";
//        String sql = "update a1 SET name = 'a',age = 31,as=jsonPath('$..book[?(@.isbn)]') where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31,as=jsonPath('$..*') where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31,as=jsonPath('$..book[?(@.price<10)]') where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31,name=false where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31.32,name=null where aa IS NULL";
//        String sql = "update a1 SET name = 'a',age = 31.32,name=null where 1.1 >= 1.1";
//        String sql = "update a1 SET name = 'a',age = 1,a1 = " +
//                "case when age = 1 then 'a' " +
//                "when age < 10 then true " +
//                "when age <20 and name = 'a' then 5.9 " +
//                "when dd is not null then null " +
////                "else toJson('{\"a\":23,\"b\":\"ab\"}') end " +
//                "else toJsonByPath('$..book[:3][\"category\"]') end " +
//                " where true = true";
        String sql = "update a1 SET name = 'a',age = 31.32,name=null where $a($b(jsonPath('$.p4'),12),'b') >= 1.1";

        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.updateSql();

        String exec = JsonSqlVisitor.exec(tree, true,jsonStr);
        System.out.println("最终结果:"+exec);
    }

    public static String exec(ParseTree tree,String jsonString){
        return exec(tree,false,jsonString);
    }

    public static String exec(ParseTree tree,boolean writeModel,String jsonString){
        JsonSqlVisitor visitor = new JsonSqlVisitor();
        visitor.setJsonString(jsonString);
        visitor.setWriteModel(writeModel);
        visitor.visit(tree);
        return visitor.getResult();
    }

    public static String exec(String sql,String jsonString){
        return exec(sql,false,jsonString);
    }

    public static String exec(String sql,boolean writeModel,String jsonString){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.updateSql();
        JsonSqlVisitor visitor = new JsonSqlVisitor();
        visitor.setJsonString(jsonString);
        visitor.setWriteModel(writeModel);
        visitor.visit(tree);
        return visitor.getResult();
    }


    private String jsonString;

    // 设置值和读取值的模式，false：直接操作原始数据，true：读在原始数据上，写在新数据上，set时，即使值被改了，读取到的还是原始的值
    private Boolean writeModel = false;

    private DocumentContext document;

    private DocumentContext newDocument;

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
        this.document = JsonPath.parse(jsonString);
        this.newDocument = JsonPath.parse(jsonString);
    }

    public String getResult(){
        if(writeModel && this.newDocument != null){
            return this.newDocument.jsonString();
        }
        if(!writeModel && this.document != null){
            return this.document.jsonString();
        }
        return this.jsonString;
    }

    @Override
    public Object visitUpdateStatement(json.sql.parse.SqlParser.UpdateStatementContext ctx) {
        //        String tableName = ctx.tableName().getText();

        // 处理 WHERE 子句
        if (ctx.expression() != null) {
            boolean result = (boolean) visit(ctx.expression());
            if (result) {
                // 处理 SET 子句
                if (ctx.setClause() != null) {
                    visit(ctx.setClause());
                }
                System.out.println("条件满足");
            } else {
                System.out.println("条件不满足");
            }
        } else {
            // 处理 SET 子句
            if (ctx.setClause() != null) {
                visit(ctx.setClause());
            }
            System.out.println("没有条件");
        }

        return null;
    }

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
        return null;
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
    public Object visitJsonPathFunc(SqlParser.JsonPathFuncContext ctx) {
        SqlParser.JsonPathFunctionContext jsonPathContext = ctx.jsonPathFunction();
        if(jsonPathContext != null){
            String jsonPath = jsonPathContext.getChild(1).getChild(1).getText();
            return read(jsonPath);
        }
        return null;
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
            sum = sum.divide(temp);
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
//        SqlParser.JsonPathFunctionContext jsonPathFunction = ctx.jsonPathFunction();
//        if(jsonPathFunction != null){
//            Object o = visitJsonPathFunction(jsonPathFunction);
//            Object read = read(o.toString());
//            if(read != null){
//                try {
//                    DocumentContext parse = JsonPath.parse(read);
//                    return parse.json();
//                }catch (Exception e){
//                    e.printStackTrace();
//                    return null;
//                }
//            }
//        }

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
            return compareValues(v1,operator,v2);
        }
        SqlParser.ExpressionContext expression = ctx.expression();
        if (expression != null) {
            return visitExpression(expression);
        }
        return false;
    }

    private boolean compareValues(Object left, String operator, Object right) {
        if (left == null || right == null) {
            return false;
        }
        switch(operator) {
            case "=":
                return left.toString().equals(right.toString());
            case "<>":
            case "!=":
                return !left.toString().equals(right.toString());
            case ">":
                return compareNumbers(left, right) > 0;
            case ">=":
                return compareNumbers(left, right) >= 0;
            case "<":
                return compareNumbers(left, right) < 0;
            case "<=":
                return compareNumbers(left, right) <= 0;
            default:
                return false;
        }
    }

    private int compareNumbers(Object left, Object right) {
        if (left instanceof Integer && right instanceof Integer) {
            return ((Integer) left).compareTo((Integer) right);
        }

        if (left instanceof Double && right instanceof Double) {
            return ((Double) left).compareTo((Double) right);
        }

        if (left instanceof Integer && right instanceof Double) {
            return Double.compare((Integer) left, (Double) right);
        }

        if (left instanceof Double && right instanceof Integer) {
            return Double.compare((Double) left, (Integer) right);
        }
        if (left instanceof BigDecimal && right instanceof BigDecimal) {
            BigDecimal v1 = (BigDecimal)left;
            BigDecimal v2 = (BigDecimal)right;
            return v1.compareTo(v2);
        }
        try {
            BigDecimal v1 = new BigDecimal(left.toString());
            BigDecimal v2 = new BigDecimal(right.toString());
            return v1.compareTo(v2);
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public Object visitIsNullExpression(SqlParser.IsNullExpressionContext ctx) {
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        // childCount=3:is null ，childCount=4:is not null
        int childCount = ctx.getChildCount();
//        DocumentContext document = JsonPath.parse(jsonString);
        try {
            Object read = document.read(columnNameContext.getText());
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
//                DocumentContext document = JsonPath.parse(jsonString);
                return document.read(jsonPath);
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
    public Object visitSetClause(SqlParser.SetClauseContext ctx) {
        List<SqlParser.SetExpressionContext> setExpressionContexts = ctx.setExpression();

        for (SqlParser.SetExpressionContext setExpression : setExpressionContexts) {
            visit(setExpression);
        }
        return null;
    }

    @Override
    public Object visitSetExpression(SqlParser.SetExpressionContext ctx) {
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        String jsonPath = (String)visitColumnName(columnNameContext);
        SqlParser.RelationalExprContext relationalExprContext = ctx.relationalExpr();
        SqlParser.CaseExprContext caseExprContext = ctx.caseExpr();
        Object value = null;
        if(relationalExprContext != null){
            value = visit(relationalExprContext);
        }
        if(caseExprContext != null){
            value = visitCaseExpr(caseExprContext);
        }

//        DocumentContext document = JsonPath.parse(jsonString);
        if (value == null) {
            try {
                if(writeModel){
                    newDocument.delete(jsonPath);
                }else{
                    document.delete(jsonPath);
                }

            }catch (PathNotFoundException e){
            }
        } else {
            try {
                if(writeModel){
                    newDocument.set(jsonPath, value);
                }else{
                    document.set(jsonPath, value);
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
                if(writeModel){
                    newDocument.put(firstPath,namePath, value);
                }else{
                    document.put(firstPath,namePath, value);
                }
            }

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
//        SqlParser.ExpressionContext expression = ctx.expression();
        SqlParser.RelationalExprContext expression = ctx.relationalExpr();
        Boolean v1 = (Boolean)visitCondition(condition);
        Object v2 = null;
        if(v1){
//            v2 = visitExpression(expression);
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
            return new BigDecimal(intValueContext.getText());
        }

        SqlParser.DoubleValueContext doubleValueContext = ctx.doubleValue();
        if (doubleValueContext != null) {
            return new BigDecimal(doubleValueContext.getText());
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
        Map<Method, List<Class<?>>> row = methodTable.row(methodName);
        Map.Entry<Method, List<Class<?>>> methodListEntry = row.entrySet().stream().findFirst().get();
        Method method = methodListEntry.getKey();
        List<Class<?>> argsTypeClasses = methodListEntry.getValue();
        List<Object> innerArgsList = new ArrayList<>();
        Object result = null;
        if(method != null){

            try {
                if(argsTypeClasses.isEmpty()){
                    result = method.invoke(null);
                }else{
                    for (int i = 0; i < argsTypeClasses.size(); i++) {
                        Class<?> aClass = argsTypeClasses.get(i);
                        Object innerArg = null;
                        if(i < innerArgs.size()){
                            innerArg = innerArgs.get(i);
                        }
                        Object convert = null;
                        try {
                            convert = Convert.convert(aClass,innerArg);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        innerArgsList.add(convert);
                    }
                    result = method.invoke(null, innerArgsList.toArray());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public Object read(String jsonPath){
        try {
            return document.read(jsonPath);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Data
    public static class WhenThenResult implements Serializable {

        private static final long serialVersionUID = 911512704921004528L;

        private Boolean conditionTrue = false;

        private Object result;
    }


}