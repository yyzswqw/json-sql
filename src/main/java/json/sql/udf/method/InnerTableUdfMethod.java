package json.sql.udf.method;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import json.sql.CurContextProxy;
import json.sql.annotation.*;
import json.sql.enums.MacroEnum;

import java.util.Map;

@UdfClass(ignoreSourceClass = true)
public class InnerTableUdfMethod {

    @UdfMethod(functionName = "registerTable",desc = "将查询语句的结果注册为一张表")
    public static boolean registerTable(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                                      @UdfParam(desc = "表名")String tableName,
                                      @UdfParam(desc = "select sql语句")String sql,
                                      @UdfParam(desc = "表的配置信息")Map<String,Object> config){
        String result = contextProxy.sql(sql);
        contextProxy.registerTable(tableName,result,config);
        return true;
    }

    @UdfMethod(functionName = "registerTableFromFilePath",desc = "将文件中的sql查询语句的结果注册为一张表")
    public static boolean registerTableFromFilePath(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                                        @UdfParam(desc = "表名")String tableName,
                                        @UdfParam(desc = "select sql语句文件路径")String filePath,
                                        @UdfParam(desc = "表的配置信息")Map<String,Object> config){
        String sql = FileUtil.readUtf8String(filePath);
        return InnerTableUdfMethod.registerTable(contextProxy, tableName, sql, config);
    }

    @UdfMethod(functionName = "dump",desc = "将表中数据导出到文件")
    public static boolean dump(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                               @UdfParam(desc = "表名")String tableName,
                               @UdfParam(desc = "导出文件路径")String filePath){
        String tableResult = contextProxy.getTableResult(tableName);
        FileUtil.writeUtf8String(tableResult,filePath);
        return true;
    }

    @UdfMethod(functionName = "dumpBySql",desc = "将查询语句的结果导出到文件")
    public static boolean dumpBySql(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                                    @UdfParam(desc = "导出文件路径")String filePath,
                                    @UdfParam(desc = "select sql语句")String sql){
        String tableResult = contextProxy.sql(sql);
        FileUtil.writeUtf8String(tableResult,filePath);
        return true;
    }

    @UdfMethod(functionName = "dumpBySqlFile",desc = "将文件中的查询语句的结果导出到文件")
    public static boolean dumpBySqlFile(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                                        @UdfParam(desc = "导出文件路径")String filePath,
                                        @UdfParam(desc = "select sql语句文件路径")String sqlFile){
        String sql = FileUtil.readUtf8String(sqlFile);
        return InnerTableUdfMethod.dumpBySql(contextProxy, sql, filePath);
    }

    @UdfMethod(functionName = "dumpAsCsv",desc = "将表中数据导出为csv")
    public static boolean dumpAsCsv(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                               @UdfParam(desc = "表名")String tableName,
                               @UdfParam(desc = "导出文件路径")String filePath,
                               @UdfParam(desc = "是否输出表头")Boolean outputHeader){
        if(ObjectUtil.isEmpty(outputHeader)){
            outputHeader = true;
        }
        String tableResult = contextProxy.getTableResult(tableName);
        String csv = contextProxy.jsonToCsv(tableResult, outputHeader);
        FileUtil.writeUtf8String(csv,filePath);
        return true;
    }

    @UdfMethod(functionName = "dumpAsCsvBySql",desc = "将查询语句的结果导出为csv")
    public static boolean dumpAsCsvBySql(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                                    @UdfParam(desc = "导出文件路径")String filePath,
                                    @UdfParam(desc = "查询sql")String sql,
                                    @UdfParam(desc = "是否输出表头")Boolean outputHeader){
        if(ObjectUtil.isEmpty(outputHeader)){
            outputHeader = true;
        }
        String tableResult = contextProxy.sql(sql);
        String csv = contextProxy.jsonToCsv(tableResult, outputHeader);
        FileUtil.writeUtf8String(csv,filePath);
        return true;
    }

    @UdfMethod(functionName = "dumpAsCsvBySqlFile",desc = "将查询sql文件的结果导出为csv")
    public static boolean dumpAsCsvBySqlFile(@MacroParam(type = MacroEnum.CUR_CONTEXT_PROXY) CurContextProxy contextProxy,
                                         @UdfParam(desc = "查询sql文件")String sqlFile,
                                         @UdfParam(desc = "导出文件路径")String filePath,
                                         @UdfParam(desc = "是否输出表头")Boolean outputHeader){
        if(ObjectUtil.isEmpty(outputHeader)){
            outputHeader = true;
        }
        String sql = FileUtil.readUtf8String(sqlFile);
        String tableResult = contextProxy.sql(sql);
        String csv = contextProxy.jsonToCsv(tableResult, outputHeader);
        FileUtil.writeUtf8String(csv,filePath);
        return true;
    }

}
