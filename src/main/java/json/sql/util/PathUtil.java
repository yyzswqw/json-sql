package json.sql.util;

import cn.hutool.core.util.ObjectUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class PathUtil {

    private PathUtil(){}

    public static void main(String[] args) throws Exception {
        String json = "{}";
//        String json = "{\"name\":[{},{},[{},{},{\"n2\":[{},{},{},{},{}]}]]}";
//        String jsonPath = "name[2][2][\"n2\"][*].a.b.c";
        String jsonPath = "$.name[2][2][\"n2\"][0:2]['b']['c']";
//        String jsonPath = "$.users[*]['b']['c']";
        DocumentContext parse = JsonPath.parse(json);
        createPath(parse, jsonPath);
        System.out.println(parse.jsonString());
    }

    public static void createPath(DocumentContext documentContext,String jsonPath){
        String[] paths = jsonPath.split("\\.");
        String parentPath = "$";
        for (int j = 0; j < paths.length; j++) {
            String path = paths[j];
            // $..name,这种写法就会是空
            if(ObjectUtil.isEmpty(path)){
                continue;
            }
            if("$".equals(path)){
                continue;
            }
            String tempJsonPath = "";
            if(path.startsWith("$")){
                tempJsonPath = path;
            }else{
                tempJsonPath = parentPath + "."+path;
            }
            Object jsonValue = getJsonValue(documentContext, tempJsonPath, Object.class);
            if(ObjectUtil.isEmpty(jsonValue)){
                if (path.contains("[")) {
                    int startFlag = 0;
                    int firstFlagIndex = 0;
                    while (path.indexOf("[",startFlag) >= 0){
                        path = path.trim();
                        String pathName = path.substring(startFlag, path.indexOf("[",startFlag));
                        firstFlagIndex = path.indexOf("]", firstFlagIndex+1);
                        String flag = path.substring(path.indexOf("[",startFlag) + 1, firstFlagIndex);
                        if(ObjectUtil.equal(parentPath,pathName)){
                            // 不需要处理
                            parentPath += "["+flag+"]";
                        }else if("*".equals(flag)){
                            if(ObjectUtil.isNotEmpty(pathName)){
                                Object value = getJsonValue(documentContext, parentPath+"."+pathName, Object.class);
                                if(ObjectUtil.isEmpty(value)){
                                    documentContext.put(parentPath,pathName, new LinkedHashMap<>());
                                }
                                parentPath += "."+pathName;
                            }
                        }else if(flag.startsWith("'") || flag.startsWith("\"")){
                            String nextKey = flag.substring(1, flag.length() - 1);
                            if(ObjectUtil.isEmpty(pathName)){
                                createPath(documentContext,parentPath +"."+nextKey);
                                parentPath += "."+nextKey;
                            }else{
                                jsonValue = getJsonValue(documentContext, parentPath+"."+pathName, Object.class);
                                if(ObjectUtil.isEmpty(jsonValue)){
                                    documentContext.put(parentPath,pathName, new LinkedHashMap<>());
                                }
                                createPath(documentContext,parentPath + "."+pathName+"."+nextKey);
                                parentPath += "."+pathName+"."+nextKey;
                            }
                        }else if(flag.contains(":")){
                            // 范围的情况，不存在时，只创建能确定数量的子对象，存在时，会对满足条件下的子对象都新增上对应的key。如 ：
                            // String json = "{\"name\":[{},{},[{},{},{\"n2\":[{},{},{},{},{}]}]]}";
                            //  String jsonPath = "$.name[2][2][\"n2\"][0:4]['b']['c']";
                            RangeParams rangeParams = parseRangeParams(flag);
                            int i = determineMaxSize(rangeParams);
                            List<Object> list = new ArrayList<>();
                            for (int i1 = 0; i1 < i; i1++) {
                                list.add(new LinkedHashMap<>());
                            }
                            if(ObjectUtil.isNotEmpty(pathName)){
                                Object value = getJsonValue(documentContext, parentPath+"."+pathName, Object.class);
                                if(ObjectUtil.isEmpty(value)){
                                    documentContext.put(parentPath,pathName, list);
                                }
                                parentPath += "."+pathName+"["+flag+"]";
                            }else{
                                Object value = getJsonValue(documentContext, parentPath, Object.class);
                                if(ObjectUtil.isEmpty(value)){
                                    documentContext.set(parentPath, list);
                                }
                                parentPath += "["+flag+"]";
                            }
                        }else{
                            // 具体数组下标的情况，但可能为负数
                            int i = Integer.parseInt(flag);
                            // 兼容负数的情况
                            i = Math.abs(i);
                            List<Object> list = new ArrayList<>();
                            for (int i1 = 0; i1 < i+1; i1++) {
                                list.add(new LinkedHashMap<>());
                            }
                            // 负数的情况，因为前面已经兼容了负数的情况，所以这里应该是不会进来的
                            if(list.isEmpty()){
                                list.add(new LinkedHashMap<>());
                            }
                            if(ObjectUtil.isNotEmpty(pathName)){
                                Object value = getJsonValue(documentContext, parentPath+"."+pathName, Object.class);
                                if(ObjectUtil.isEmpty(value)){
                                    documentContext.put(parentPath,pathName, list);
                                }
                                parentPath += "."+pathName+"["+flag+"]";
                            }else{
                                Object value = getJsonValue(documentContext, parentPath, Object.class);
                                if(ObjectUtil.isEmpty(value)){
                                    documentContext.set(parentPath, list);
                                }
                                parentPath += "["+flag+"]";
                            }
                        }

                        if(path.trim().length()-1 != firstFlagIndex){
                            startFlag = firstFlagIndex +1;
                        }else{
                            path = "";
                        }
                    }
                }else{
                    Object value = getJsonValue(documentContext, parentPath+"."+path, Object.class);
                    if(ObjectUtil.isEmpty(value)){
                        documentContext.put(parentPath,path, new LinkedHashMap<>());
                    }
                    parentPath += "."+path;
                }

            }else{
                parentPath = tempJsonPath;
            }
        }
    }

    public static <T> T getJsonValue(DocumentContext jsonDocument, String jsonPath, Class<T> clazz) {
        try {
            return jsonDocument.read( jsonPath,clazz);
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 解析范围参数
     * 支持格式：[start:end], [start:], [:end], [::step], [start:end:step]
     */
    private static RangeParams parseRangeParams(String flag) {
        // 移除方括号（如果存在）
        String rangeStr = flag;
        if (flag.startsWith("[") && flag.endsWith("]")) {
            rangeStr = flag.substring(1, flag.length() - 1);
        }
        // 分割参数
        String[] parts = rangeStr.split(":", -1); // 使用-1保留空字符串

        Integer start = null;
        Integer end = null;
        Integer step = null;
        // 根据参数数量解析
        if (parts.length >= 1) {
            start = parseRangeValue(parts[0]);
        }
        if (parts.length >= 2) {
            end = parseRangeValue(parts[1]);
        }
        if (parts.length >= 3) {
            step = parseRangeValue(parts[2]);
        }
        return new RangeParams(start, end, step);
    }

    /**
     * 解析范围值，支持负数
     */
    private static Integer parseRangeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 确定范围的最大值
     * 根据范围参数确定需要生成的对象数量，无法确定最大值时生成最小需要的数量
     */
    private static int determineMaxSize(RangeParams params) {
        // 默认最小大小为1
        int maxSize = 1;

        // 处理各种范围情况，优先处理能确定最大值的情况
        if (params.hasStart() && params.hasEnd()) {
            // [start:end] 或 [start:end:step] 格式
            int start = params.getStart() != null ? params.getStart() : 0;
            int end = params.getEnd() != null ? params.getEnd() : 0;

            // 处理负数范围 - 无法确定确切的最大值，生成最小需要的
            if (start < 0 || end < 0) {
                // 负数范围，如 [-3:-1]，无法确定确切大小
                // 生成最小需要的对象：覆盖负数索引所需的最小数量
                int absStart = Math.abs(start);
                int absEnd = Math.abs(end);
                maxSize = Math.max(absStart, absEnd) + 1; // 最小需要的数量
            } else {
                // 正数范围，可以确定确切的最大值
                maxSize = Math.max(start, end) + 1;
            }

        } else if (params.hasStart() && !params.hasEnd()) {
            // [start:] 格式 - 从start开始到末尾，无法确定确切大小
            int start = params.getStart() != null ? params.getStart() : 0;
            if (start < 0) {
                // 负数开始，如 [-3:]，无法确定确切大小
                // 生成最小需要的对象：覆盖负数索引所需的最小数量
                maxSize = Math.abs(start) + 1;
            } else {
                // 正数开始，如 [3:]，无法知道末尾在哪里
                // 生成最小需要的对象：只生成起始位置的对象
                maxSize = start + 1; // 最小需要的数量
            }

        } else if (!params.hasStart() && params.hasEnd()) {
            // [:end] 格式 - 从0到end
            int end = params.getEnd() != null ? params.getEnd() : 0;
            if (end < 0) {
                // 负数结束，如 [:-3]，无法确定确切大小
                // 生成最小需要的对象：覆盖负数索引所需的最小数量
                maxSize = Math.abs(end) + 1;
            } else {
                // 正数结束，可以确定确切的最大值
                maxSize = end + 1;
            }

        } else if (params.hasStep()) {
            // [::step] 格式 - 步长模式，无法确定确切大小
            // 生成最小需要的对象：只生成第一个步长的对象
            int step = params.getStep() != null ? Math.abs(params.getStep()) : 1;
            maxSize = step; // 最小需要的数量

        } else if (!params.hasStart() && !params.hasEnd() && !params.hasStep()) {
            // [:] 格式 - 全范围，无法确定确切大小
            // 生成最小需要的对象：只生成一个对象
            maxSize = 1;
        } else {
            // 其他未知格式，使用最小需要的数量
            maxSize = 1;
        }

        // 确保最小大小为1，不设置上限，是多少就是多少
        return Math.max(1, maxSize);
    }

    /**
     * 范围参数类
     */
    public static class RangeParams {
        private final Integer start;
        private final Integer end;
        private final Integer step;

        public RangeParams(Integer start, Integer end, Integer step) {
            this.start = start;
            this.end = end;
            this.step = step;
        }

        public Integer getStart() { return start; }
        public Integer getEnd() { return end; }
        public Integer getStep() { return step; }

        public boolean hasStart() { return start != null; }
        public boolean hasEnd() { return end != null; }
        public boolean hasStep() { return step != null; }
    }



}
