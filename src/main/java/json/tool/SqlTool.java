package json.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import json.sql.JsonSqlContext;
import json.tool.param.JobParameters;
import json.tool.util.JobParamUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;


@Slf4j
public class SqlTool {

    private static final JsonSqlContext jsonSqlContext = JsonSqlContext.builder().build();

    public static void main(String[] args) throws Exception {

        JobParameters jobParameters = new JobParameters();
        if (JobParamUtil.parseParam(args, jobParameters, SqlTool.class)) {
            log.error("parse args error");
            return;
        }
        if(ObjectUtil.isNotEmpty(jobParameters.getJsonData())){
            jsonSqlContext.registerTable(jobParameters.getTableName(),jobParameters.getJsonData());
        }else{
            jsonSqlContext.registerTable(jobParameters.getTableName(),new File(jobParameters.getFile()));
        }
        String result = null;
        if(ObjectUtil.isNotEmpty(jobParameters.getSqlFile())){
            result = jsonSqlContext.sql(new File(jobParameters.getSqlFile()));
        }else if(ObjectUtil.isNotEmpty(jobParameters.getSql())){
            result = jsonSqlContext.sql(jobParameters.getSql());
        }else {
            throw new RuntimeException("sql is null，use --sql or --sqlFile");
        }

        String format = jobParameters.getFormat();
        String outputFile = jobParameters.getOutputFile();
        if(ObjectUtil.isEmpty(format) || format.equalsIgnoreCase("non") || format.equalsIgnoreCase("json")){
            if(ObjectUtil.isNotEmpty(outputFile)){
                FileUtil.writeUtf8String(result,outputFile);
            }else{
                Console.log(result);
            }
        }else if(format.equalsIgnoreCase("csv")){
            String csv = jsonSqlContext.jsonToCsv(result,!jobParameters.isNoOutputHeader());
            if(ObjectUtil.isNotEmpty(outputFile)){
                FileUtil.writeUtf8String(csv,outputFile);
            }else{
                Console.log(csv);
            }
        }else{
            if(ObjectUtil.isNotEmpty(outputFile)){
                FileUtil.writeUtf8String(result,outputFile);
            }else{
                Console.log(result);
            }
        }
    }
}
