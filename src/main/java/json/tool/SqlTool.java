package json.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import json.sql.JsonSqlContext;
import json.tool.param.JobParameters;
import json.tool.util.JobParamUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.reflections.vfs.Vfs;

import java.io.*;
import java.util.*;


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

    public static String convertJsonToCsv(JsonNode jsonNode,boolean outputHeader) throws IOException {
        if(Objects.isNull(jsonNode)){
            return "";
        }
        if(jsonNode instanceof NumericNode){
            return jsonNode.asText();
        }else if(jsonNode instanceof TextNode){
            return jsonNode.asText();
        }else if(jsonNode instanceof POJONode){
            return jsonNode.asText();
        }else if(jsonNode instanceof ValueNode){
            return jsonNode.asText();
        }else if(jsonNode instanceof ArrayNode){
            StringWriter stringWriter = new StringWriter();
            CSVPrinter csvPrinter = null;

            ArrayNode arrayNode = (ArrayNode)jsonNode;
            JsonNode firstObject = arrayNode.get(0);
            Iterator<Map.Entry<String, JsonNode>> fields = firstObject.fields();

            // Extract headers
            if(outputHeader){
                List<String> headerList = new ArrayList<>();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    headerList.add(key);
                }
                csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT.withHeader(headerList.toArray(new String[0])));
            }else{
                csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT);
            }

            // Extract rows
            int size = arrayNode.size();
            for (int i = 0; i < size; i++) {
                JsonNode jsonObject = arrayNode.get(i);
                fields = jsonObject.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    JsonNode value = field.getValue();
                    if(value instanceof ObjectNode || value instanceof ArrayNode){
                        String val = jsonSqlContext.toJsonString(value);
                        csvPrinter.print(val);
                    }else{
                        csvPrinter.print(value.asText());
                    }
                }
                csvPrinter.println();
            }
            csvPrinter.close();
            return stringWriter.toString();
        }else if(jsonNode instanceof ObjectNode){
            StringWriter stringWriter = new StringWriter();
            CSVPrinter csvPrinter = null;

            ObjectNode jsonObject = (ObjectNode)jsonNode;
            Iterator<Map.Entry<String, JsonNode>> fields = jsonObject.fields();

            // Extract headers
            if(outputHeader){
                List<String> headerList = new ArrayList<>();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    headerList.add(key);
                }
                csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT.withHeader(headerList.toArray(new String[0])));
            }else{
                csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT);
            }

            fields = jsonObject.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                if(value instanceof ObjectNode || value instanceof ArrayNode){
                    String val = jsonSqlContext.toJsonString(value);
                    csvPrinter.print(val);
                }else{
                    csvPrinter.print(value.asText());
                }
            }
            csvPrinter.close();
            return stringWriter.toString();
        }
        return jsonNode.asText();
    }

}
