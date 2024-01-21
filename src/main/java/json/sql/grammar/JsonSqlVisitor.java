package json.sql.grammar;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.jayway.jsonpath.*;
import json.sql.config.TableConfig;
import json.sql.entity.TableContext;
import json.sql.enums.MacroEnum;
import json.sql.parse.SqlBaseVisitor;
import json.sql.parse.SqlParser;
import json.sql.udf.CustomMethod;
import json.sql.util.CompareUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

@Data
@Slf4j
public class JsonSqlVisitor extends SqlBaseVisitor<Object> {

    private static Table<String,Method,List<Class<?>>> methodTable = HashBasedTable.create();
    private static Map<String,List<MacroEnum>> macroMap = Maps.newHashMap();

    private static Map<String, TableContext> tableDataMap = Maps.newHashMap();
    private static Stack<String> tableNameStack = new Stack<>();
    private static final String MAIN_TABLE_NAME = "__$$main_table_name$$__";

    private static final Pattern colNamePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

    // region ======================== api start ===================================

    public static void registerFunction(String functionName,Method method,Class<?> ... argsType){
        if(argsType == null || argsType.length == 0){
            methodTable.put(functionName,method,new ArrayList<>());
        }else{
            if (methodTable.containsRow(functionName)) {
                throw new RuntimeException("已存在 function : "+functionName);
            }
            methodTable.put(functionName,method,Arrays.asList(argsType));
        }
    }

    public static void registerMacro(String functionName,MacroEnum ... macros) {
        if(macros == null || macros.length == 0){
            return ;
        }else{
            if (macroMap.containsKey(functionName)) {
                throw new RuntimeException("已存在 macro : "+functionName);
            }
            macroMap.put(functionName,Arrays.asList(macros));
        }
    }

    public static void registerTable(String tableName,String json,Map<String,Object> config) {
        if(ObjectUtil.hasEmpty(tableName,json)){
            throw new RuntimeException("表名或者json 数据不能为空");
        }
        TableContext tableContext = tableDataMap.get(tableName);
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
        tableDataMap.put(tableName,tableContext);
    }

    public static void registerTable(String tableName,String json) {
        registerTable(tableName,json,null);
    }

    public static void setTableConfig(String tableName,String key,Object value) {
        Map<String, Object> tableContextConfig = getTableContextConfig(tableName);
        if(tableContextConfig != null){
            tableContextConfig.put(key,value);
        }
    }

    public static String exec(ParseTree tree){
        return exec(tree,null,null);
    }

    public static String exec(ParseTree tree,String jsonString){
        return exec(tree,null,jsonString);
    }

    public static String exec(ParseTree tree,Boolean writeModel){
        return exec(tree,writeModel,null);
    }

    public static String exec(ParseTree tree,Boolean writeModel,String jsonString){
        JsonSqlVisitor visitor = new JsonSqlVisitor();
        if(ObjectUtil.isNotEmpty(jsonString)){
            visitor.setJsonString(jsonString);
        }
        if(ObjectUtil.isNotEmpty(writeModel)){
            JsonSqlVisitor.setWriteModel(MAIN_TABLE_NAME,writeModel);
        }
        Object visit = visitor.visit(tree);
        if(ObjectUtil.isNotEmpty(visit)){
            return visit == null ? "{}":visit.toString();
        }
        return JsonSqlVisitor.getResult(MAIN_TABLE_NAME);
    }

    public static String exec(String sql){
        return exec(sql,false,null);
    }

    public static String exec(String sql,String jsonString){
        return exec(sql,false,jsonString);
    }

    public static String exec(String sql,boolean writeModel){
        return exec(sql,writeModel,null);
    }

    public static String exec(String sql,boolean writeModel,String jsonString){
        json.sql.parse.SqlLexer lexer = new json.sql.parse.SqlLexer(CharStreams.fromString(sql));
        json.sql.parse.SqlParser parser = new json.sql.parse.SqlParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.updateSql();
        JsonSqlVisitor visitor = new JsonSqlVisitor();
        if(ObjectUtil.isNotEmpty(jsonString)){
            visitor.setJsonString(jsonString);
        }
        if(ObjectUtil.isNotEmpty(writeModel)){
            JsonSqlVisitor.setWriteModel(MAIN_TABLE_NAME,writeModel);
        }
        Object visit = visitor.visit(tree);
        if(tree instanceof SqlParser.SelectStatementContext){
            return visit == null ? "{}":visit.toString();
        }
        return JsonSqlVisitor.getResult(MAIN_TABLE_NAME);
    }


    public void setJsonString(String jsonString) {
        registerTable(MAIN_TABLE_NAME,jsonString);
    }

    public static void setWriteModel(String tableName,boolean writeModel) {
        TableContext tableContext = getTableContext(tableName);
        if(ObjectUtil.isEmpty(tableContext)){
            registerTable(tableName,"{}");
        }
        tableContext = getTableContext(tableName);
        tableContext.setConfig(TableConfig.WRITE_MODEL,writeModel);
    }

    public static String getResult(String tableName){
        TableContext tableContext = tableDataMap.get(tableName);
        if(ObjectUtil.isEmpty(tableContext)){
            return null;
        }
        final Boolean writeModel = MapUtil.getBool(tableContext.getConfig(), TableConfig.WRITE_MODEL
                , MapUtil.getBool(TableContext.defaultConfig, TableConfig.WRITE_MODEL, false));
        if(writeModel && tableContext.getNewDocument() != null){
            return tableContext.getNewDocument().jsonString();
        }
        if(!writeModel && tableContext.getDocument() != null){
            return tableContext.getDocument().jsonString();
        }
        return tableContext.getOriginalJson();
    }

    public static TableContext getTableContext(String tableName){
        return tableDataMap.get(tableName);
    }

    public static Map<String,Object> getTableContextConfig(String tableName){
        TableContext tableContext = getTableContext(tableName);
        return tableContext == null?null:tableContext.getConfig();
    }

    public static <T> T getTableContextConfig(String tableName,String key,Class<T> classType){
        TableContext tableContext = getTableContext(tableName);
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

    public static <T> T getTableContextConfig(String tableName,String key,Class<T> classType,T defaultValue){
        TableContext tableContext = getTableContext(tableName);
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

    public static DocumentContext getTableContextDocument(String tableName){
        TableContext tableContext = getTableContext(tableName);
        return tableContext == null?null:tableContext.getDocument();
    }

    public static DocumentContext getTableContextNewDocument(String tableName){
        TableContext tableContext = getTableContext(tableName);
        return tableContext == null?null:tableContext.getNewDocument();
    }

    public static String getTableContextOriginalJson(String tableName){
        TableContext tableContext = getTableContext(tableName);
        return tableContext == null?null:tableContext.getOriginalJson();
    }

    public static int delCol(String tableName,String jsonPath){
        TableContext tableContext = getTableContext(tableName);
        if(ObjectUtil.isEmpty(tableContext)){
            return 0;
        }
        try {
            Boolean writeModel = getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
            if(writeModel){
                getTableContextNewDocument(tableName).delete(jsonPath);
            }else{
                getTableContextDocument(tableName).delete(jsonPath);
            }
            return 1;
        }catch (PathNotFoundException e){
        }
        return 0;
    }

    public Object read(String jsonPath){
        try {
            String tableName = tableNameStack.peek();
            return getTableContextDocument(tableName).read(jsonPath);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

// endregion ======================== api end ===================================

    @Override
    public Object visitSql(SqlParser.SqlContext ctx) {
        SqlParser.UpdateStatementContext updateStatementContext = ctx.updateStatement();
        SqlParser.SelectStatementContext selectStatementContext = ctx.selectStatement();
        SqlParser.DeleteStatementContext deleteStatementContext = ctx.deleteStatement();
        if(ObjectUtil.isNotEmpty(updateStatementContext)){
            return visitUpdateStatement(updateStatementContext);
        }
        if(ObjectUtil.isNotEmpty(selectStatementContext)){
            return visitSelectStatement(selectStatementContext);
        }
        if(ObjectUtil.isNotEmpty(deleteStatementContext)){
            return visitDeleteStatement(deleteStatementContext);
        }
        return null;
    }

    // region ======================== update start ===================================
    @Override
    public Object visitUpdateStatement(SqlParser.UpdateStatementContext ctx) {
        SqlParser.TableNameContext tableNameContext = ctx.tableName();
        if(ObjectUtil.isNotEmpty(tableNameContext)){
            String tableName = tableNameContext.getText();
            if (!tableDataMap.containsKey(tableName)) {
                throw new RuntimeException("not exist table : "+tableName);
            }
            tableNameStack.push(tableName);
        }else{
            tableNameStack.push(MAIN_TABLE_NAME);
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
                System.out.println("条件满足");
                return 0;
            } else {
                System.out.println("条件不满足");
                return 0;
            }
        } else {
            // 处理 SET 子句
            if (ctx.setClause() != null) {
                visit(ctx.setClause());
            }
            System.out.println("没有条件");
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
        SqlParser.ColumnNameContext columnNameContext = ctx.columnName();
        String jsonPath = (String)visitColumnName(columnNameContext);
        SqlParser.RelationalExprContext relationalExprContext = ctx.relationalExpr();
        SqlParser.CaseExprContext caseExprContext = ctx.caseExpr();
        SqlParser.DelColumnExprContext delColumnExprContext = ctx.delColumnExpr();
        String tableName = tableNameStack.peek();
        if(delColumnExprContext != null){
            delCol(tableName,jsonPath);
            return null;
        }
        Object value = null;
        if(relationalExprContext != null){
            value = visit(relationalExprContext);
        }
        if(caseExprContext != null){
            value = visitCaseExpr(caseExprContext);
        }

        try {
            Boolean writeModel = getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
            if(writeModel){
                getTableContextNewDocument(tableName).set(jsonPath, value);
            }else{
                getTableContextDocument(tableName).set(jsonPath, value);
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
            Boolean writeModel = getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
            if(writeModel){
                getTableContextNewDocument(tableName).put(firstPath,namePath, value);
            }else{
                getTableContextDocument(tableName).put(firstPath,namePath, value);
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
        Map<Method, List<Class<?>>> row = methodTable.row(methodName);
        if(row == null || row.isEmpty()){
            throw new RuntimeException("not has function : "+methodName);
        }
        Map.Entry<Method, List<Class<?>>> methodListEntry = row.entrySet().stream().findFirst().get();
        Method method = methodListEntry.getKey();
        List<Class<?>> argsTypeClasses = methodListEntry.getValue();
        List<Object> innerArgsList = new ArrayList<>();
        Object result = null;
        if(method != null){
            List<MacroEnum> macroEnums = macroMap.get(methodName);
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
    public Object visitStringValue(SqlParser.StringValueContext ctx) {
        String text = ctx.getText();
        return text.substring(1,text.length() -1);
    }

    // endregion ======================== 字面量 end ===================================


    //region    ====================== select start ============================

    @Override
    public Object visitSelectStatement(SqlParser.SelectStatementContext ctx) {
        SqlParser.SelectListContext selectListContext = ctx.selectList();
        SqlParser.TableNameContext tableNameContext = ctx.tableName();
        SqlParser.ExpressionContext expression = ctx.expression();
        String tableName = tableNameContext.getText();
        tableNameStack.push(tableName);
        boolean condition = true;
        if(ObjectUtil.isNotEmpty(expression)){
            condition = (Boolean) visitExpression(expression);
        }
        if(!condition){
            tableNameStack.pop();
            return null;
        }
        Object v = visitSelectList(selectListContext);
        tableNameStack.pop();
        return v == null ? "{}":v.toString();
    }

    @Override
    public Object visitStarLable(SqlParser.StarLableContext ctx) {
        return read("$");
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
                    if(read instanceof List){
                        List tempV = (List)read;
                        if(ObjectUtil.isNotEmpty(tempV)){
                            inValueList.addAll(tempV);
                        }
                    }else{
                        inValueList.add(read);
                    }
                }catch (Exception e){

                }

            }
        }

        for (Object v : inValueList) {
            int i = CompareUtil.compareValue(v1, v);
            if(ObjectUtil.isNotEmpty(notLableContext)){
                if(i==0){
                    return false;
                }
            }else{
                if(i==0){
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
            }catch (Exception e){
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

        int flag0 = CompareUtil.compareValue(v1, v2);
        int flag1 = CompareUtil.compareValue(v1, v3);
        if(ObjectUtil.isNotEmpty(notLableContext)){
            if (flag0 < 0 || flag1 > 0) {
                return true;
            }
            return false;
        }else{
            if (flag0 >= 0 && flag1 <= 0) {
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
            tableNameStack.push(tableName);
        }else{
            tableNameStack.push(MAIN_TABLE_NAME);
        }
        boolean condition = true;
        if(ObjectUtil.isNotEmpty(expression)){
            condition = (Boolean) visitExpression(expression);
        }

        if(!condition){
            tableNameStack.pop();
            return 0;
        }
        Integer flag = (Integer)visitDelClause(delClauseContext);
        tableNameStack.pop();
        return flag;
    }

    @Override
    public Object visitDelClause(SqlParser.DelClauseContext ctx) {
        List<SqlParser.ColumnNameContext> columnNameContexts = ctx.columnName();
        String tableName = tableNameStack.peek();
        for (SqlParser.ColumnNameContext columnNameContext : columnNameContexts) {
            Object jsonPath = visitColumnName(columnNameContext);
            delCol(tableName,jsonPath.toString());
        }
        return 1;
    }

    //endregion ====================== delete end  ============================

    //region   ====================== 宏定义 start ============================
    private Object getMacro(MacroEnum macroEnum) {
        String tableName = tableNameStack.peek();
        if(macroEnum != null){
            switch (macroEnum){
                case ORIGINAL_JSON:
                    return getTableContextOriginalJson(tableName);
                case READ_DOCUMENT:
                case ORIGINAL_WRITE_DOCUMENT:
                    return getTableContextDocument(tableName);
                case COPY_WRITE_WRITE_DOCUMENT:
                    return getTableContextNewDocument(tableName);
                case CUR_WRITE_DOCUMENT:
                    Boolean writeModel = getTableContextConfig(tableName, TableConfig.WRITE_MODEL, Boolean.class,false);
                    return writeModel ? getTableContextNewDocument(tableName) : getTableContextDocument(tableName);
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