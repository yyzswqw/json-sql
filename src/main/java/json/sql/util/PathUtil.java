package json.sql.util;

import cn.hutool.core.util.ObjectUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON 路径创建工具
 * 支持在空 JSON 上根据路径自动创建嵌套结构
 * 
 * <p>支持的路径格式：</p>
 * <ul>
 *   <li>普通路径：$.a.b.c</li>
 *   <li>数组索引：$.arr[0].b.c</li>
 *   <li>连续数组索引：$.arr[2][2].b.c</li>
 *   <li>通配符 [*]：$.users[*].name</li>
 *   <li>通配符 *：$.users.*.name</li>
 *   <li>范围 [start:end]：$.arr[0:3].name</li>
 *   <li>负数索引：$.arr[-1].name</li>
 *   <li>引号键名：$["key-with-dashes"].name</li>
 * </ul>
 */
public class PathUtil {

    private static final Pattern NEGATIVE_INDEX_PATTERN = Pattern.compile("\\[(-\\d+)]");

    private PathUtil() {}

    // ==================== 主入口 ====================

    /**
     * 在 JSON 文档上创建指定路径的嵌套结构
     * 
     * @param documentContext JSON 文档上下文
     * @param jsonPath JSON 路径，如 "$.users[*].name" 或 "a.b.c"
     */
    public static void createPath(DocumentContext documentContext, String jsonPath) {
        String[] paths = jsonPath.split("\\.");
        String parentPath = "$";

        for (int j = 0; j < paths.length; j++) {
            String path = paths[j];

            // 跳过空路径和根路径
            if (ObjectUtil.isEmpty(path) || "$".equals(path)) {
                continue;
            }

            // 构建完整路径
            String tempJsonPath = path.startsWith("$") ? path : parentPath + "." + path;

            // 如果 parentPath 包含通配符，跳过读取（通配符路径无法直接读取）
            boolean hasWildcard = parentPath.contains("[*]");
            Object jsonValue = hasWildcard ? null : getJsonValue(documentContext, tempJsonPath, Object.class);

            // 如果值不存在，创建路径
            if (ObjectUtil.isEmpty(jsonValue)) {
                if (path.contains("[")) {
                    parentPath = processPathWithBrackets(documentContext, path, parentPath, j, paths);
                } else {
                    processPathWithoutBrackets(documentContext, path, parentPath, j, paths);
                    parentPath = appendToParentPath(parentPath, path);
                }
            } else {
                // 值已存在，更新 parentPath
                parentPath = tempJsonPath;
            }
        }
    }

    // ==================== 路径段处理 ====================

    /**
     * 处理包含方括号的路径段（如 "arr[0][2].name[*]"）
     *
     * @return 更新后的 parentPath
     */
    private static String processPathWithBrackets(DocumentContext ctx, String originalPath, String parentPath, int j, String[] paths) {
        String path = originalPath.trim();
        int startFlag = 0;
        int firstFlagIndex = 0;
        String resultPath = parentPath;

        while (path.indexOf("[", startFlag) >= 0) {
            path = path.trim();
            String pathName = path.substring(startFlag, path.indexOf("[", startFlag));
            firstFlagIndex = path.indexOf("]", firstFlagIndex + 1);
            String flag = path.substring(path.indexOf("[", startFlag) + 1, firstFlagIndex);

            // 判断当前方括号是否是路径段中的最后一个
            boolean isLastBracket = !hasMoreBrackets(path, firstFlagIndex + 1);

            // 根据 flag 类型分发处理
            if (ObjectUtil.equal(resultPath, pathName)) {
                handleParentPathEqualsPathName(ctx, resultPath, flag);
                resultPath += "." + "[" + flag + "]";
            } else if ("*".equals(flag)) {
                handleWildcard(ctx, resultPath, pathName);
                resultPath = appendWildcard(resultPath, pathName);
            } else if (flag.startsWith("'") || flag.startsWith("\"")) {
                handleQuotedKey(ctx, resultPath, pathName, flag, j, paths);
                resultPath = appendQuotedKey(resultPath, pathName, flag);
            } else if (flag.contains(":")) {
                handleRange(ctx, resultPath, pathName, flag, isLastBracket, j, paths);
                resultPath = appendBracket(resultPath, pathName, flag);
            } else {
                handleNumericIndex(ctx, resultPath, pathName, flag, isLastBracket, j, paths);
                resultPath = appendBracket(resultPath, pathName, flag);
            }

            // 更新循环位置
            if (path.trim().length() - 1 != firstFlagIndex) {
                startFlag = firstFlagIndex + 1;
            } else {
                path = "";
            }
        }

        return resultPath;
    }

    /**
     * 处理不带方括号的路径段
     */
    private static void processPathWithoutBrackets(DocumentContext ctx, String path, String parentPath, int j, String[] paths) {
        // 处理通配符 *（如 $.users.*.b.c 中的 *）
        if ("*".equals(path)) {
            ensureArrayForWildcard(ctx, parentPath);
        } else {
            // 普通键名，创建空对象
            Object value = getJsonValue(ctx, parentPath + "." + path, Object.class);
            if (ObjectUtil.isEmpty(value)) {
                String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
                ctx.put(newParentPath, path, new LinkedHashMap<>());
            }
        }
    }

    // ==================== 方括号内类型处理 ====================

    /**
     * 处理 pathName 等于 parentPath 的情况（连续数组索引）
     */
    private static void handleParentPathEqualsPathName(DocumentContext ctx, String parentPath, String flag) {
        Object jsonValue = getJsonValue(ctx, parentPath + "." + "[" + flag + "]", Object.class);
        if (ObjectUtil.isEmpty(jsonValue)) {
            createPath(ctx, parentPath + "." + "[" + flag + "]");
        }
    }

    /**
     * 处理通配符 [*]
     * 将当前路径的值转换为数组，以便后续通配符匹配
     */
    private static void handleWildcard(DocumentContext ctx, String parentPath, String pathName) {
        // 如果有 pathName，先确保 pathName 对应的键存在
        if (ObjectUtil.isNotEmpty(pathName)) {
            Object value = getJsonValue(ctx, parentPath + "." + pathName, Object.class);
            if (ObjectUtil.isEmpty(value)) {
                String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
                ctx.put(newParentPath, pathName, new LinkedHashMap<>());
            }
            parentPath += "." + pathName;
        }

        // 将当前路径的值转换为数组
        ensureArrayForWildcard(ctx, parentPath);
    }

    /**
     * 确保指定路径的值是数组（用于通配符处理）
     */
    private static void ensureArrayForWildcard(DocumentContext ctx, String parentPath) {
        Object currentValue = getJsonValue(ctx, parentPath, Object.class);
        String newParentPath = safeResolveNegativeIndices(ctx, parentPath);

        if (ObjectUtil.isNotEmpty(currentValue) && !(currentValue instanceof List)) {
            // 当前值是对象，转换为数组的第一个元素
            List<Object> array = new ArrayList<>();
            array.add(currentValue);
            ctx.set(newParentPath, array);
        } else if (ObjectUtil.isEmpty(currentValue)) {
            // 值不存在，创建一个包含空对象的数组
            List<Object> array = new ArrayList<>();
            array.add(new LinkedHashMap<>());
            ctx.set(newParentPath, array);
        } else if (currentValue instanceof List) {
            // 是数组但为空，添加一个空对象元素
            List<Object> arr = (List<Object>) currentValue;
            if (arr.isEmpty()) {
                arr.add(new LinkedHashMap<>());
                ctx.set(newParentPath, arr);
            }
        }
    }

    /**
     * 处理引号键名（如 ["key-name"]）
     */
    private static void handleQuotedKey(DocumentContext ctx, String parentPath, String pathName, String flag, int j, String[] paths) {
        String nextKey = flag.substring(1, flag.length() - 1);

        if (ObjectUtil.isEmpty(pathName)) {
            createPath(ctx, parentPath + "." + nextKey);
        } else {
            Object jsonValue = getJsonValue(ctx, parentPath + "." + pathName, Object.class);
            if (ObjectUtil.isEmpty(jsonValue)) {
                String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
                ctx.put(newParentPath, pathName, new LinkedHashMap<>());
            }
            createPath(ctx, parentPath + "." + pathName + "." + nextKey);
        }
    }

    /**
     * 处理范围索引（如 [0:3]、[start:end]）
     */
    private static void handleRange(DocumentContext ctx, String parentPath, String pathName, String flag, boolean isLastBracket, int j, String[] paths) {
        RangeParams rangeParams = parseRangeParams(flag);
        int size = determineMaxSize(rangeParams);

        if (ObjectUtil.isNotEmpty(pathName)) {
            createOrExpandArray(ctx, parentPath, pathName, size, true, isLastBracket, j, paths);
        } else {
            createOrExpandArrayAtParent(ctx, parentPath, size, true, isLastBracket, j, paths);
        }
    }

    /**
     * 处理数字索引（如 [2]、[0]、[-1]）
     */
    private static void handleNumericIndex(DocumentContext ctx, String parentPath, String pathName, String flag, boolean isLastBracket, int j, String[] paths) {
        int rawIndex = Integer.parseInt(flag);
        // 负数索引：-1 表示最后一个元素，数组不存在时创建 |index| 个元素
        // 正数索引：2 表示索引 2，数组不存在时创建 3 个元素（0,1,2）
        int arraySize = rawIndex < 0 ? Math.abs(rawIndex) : rawIndex + 1;

        if (ObjectUtil.isNotEmpty(pathName)) {
            createOrExpandArray(ctx, parentPath, pathName, arraySize, false, isLastBracket, j, paths);
        } else {
            createOrExpandArrayAtParent(ctx, parentPath, arraySize, false, isLastBracket, j, paths);
        }
    }

    // ==================== 数组创建/扩展 ====================

    /**
     * 在指定键名上创建或扩展数组
     *
     * @param ctx JSON 上下文
     * @param parentPath 父路径
     * @param pathName 键名
     * @param arraySize 数组大小
     * @param isRange 是否是范围索引
     * @param isLastBracket 是否是最后一个方括号
     * @param j 当前路径段索引
     * @param paths 所有路径段
     */
    private static void createOrExpandArray(DocumentContext ctx, String parentPath, String pathName, int arraySize, boolean isRange, boolean isLastBracket, int j, String[] paths) {
        Object value = getJsonValue(ctx, parentPath + "." + pathName, Object.class);

        if (ObjectUtil.isEmpty(value)) {
            // 数组不存在，创建
            List<Object> list = createArrayWithNestedStructure(arraySize, isLastBracket, j, paths);
            String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
            ctx.put(newParentPath, pathName, list);
        } else if (value instanceof List) {
            // 数组已存在，扩展
            List<Object> existingList = (List<Object>) value;
            int currentSize = existingList.size();
            int targetSize = Math.max(currentSize, arraySize);

            for (int idx = currentSize; idx < targetSize; idx++) {
                // 只有最后一个索引才创建后续嵌套结构，中间的元素为空对象
                boolean isTargetIndex = (idx == arraySize - 1);
                existingList.add(isTargetIndex ? createNestedElement(isLastBracket, j, paths) : new LinkedHashMap<>());
            }

            String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
            ctx.put(newParentPath, pathName, existingList);
        }
    }

    /**
     * 在父路径上创建或扩展数组（pathName 为空的情况）
     */
    private static void createOrExpandArrayAtParent(DocumentContext ctx, String parentPath, int arraySize, boolean isRange, boolean isLastBracket, int j, String[] paths) {
        Object value = getJsonValue(ctx, parentPath, Object.class);

        if (ObjectUtil.isEmpty(value)) {
            // 数组不存在，创建
            List<Object> list = createArrayWithNestedStructure(arraySize, isLastBracket, j, paths);
            String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
            ctx.set(newParentPath, list);
        } else if (value instanceof List) {
            // 数组已存在，扩展
            List<Object> existingList = (List<Object>) value;
            int currentSize = existingList.size();
            int targetSize = Math.max(currentSize, arraySize);

            for (int idx = currentSize; idx < targetSize; idx++) {
                // 只有最后一个索引才创建后续嵌套结构，中间的元素为空对象
                boolean isTargetIndex = (idx == arraySize - 1);
                existingList.add(isTargetIndex ? createNestedElement(isLastBracket, j, paths) : new LinkedHashMap<>());
            }

            String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
            ctx.set(newParentPath, existingList);
        } else {
            // 当前值是对象而不是数组，转换为数组
            List<Object> newArray = convertObjectToArray(value, arraySize, isLastBracket, j, paths);
            String newParentPath = safeResolveNegativeIndices(ctx, parentPath);
            ctx.set(newParentPath, newArray);
        }
    }

    /**
     * 创建包含嵌套结构的数组
     */
    private static List<Object> createArrayWithNestedStructure(int size, boolean isLastBracket, int j, String[] paths) {
        List<Object> list = new ArrayList<>();
        for (int idx = 0; idx < size; idx++) {
            list.add(createNestedElement(isLastBracket, j, paths));
        }
        return list;
    }

    /**
     * 创建单个嵌套元素（包含后续路径的嵌套结构）
     */
    private static LinkedHashMap<String, Object> createNestedElement(boolean isLastBracket, int j, String[] paths) {
        LinkedHashMap<String, Object> element = new LinkedHashMap<>();

        // 只有最后一个方括号才创建后续嵌套结构
        if (isLastBracket && j + 1 < paths.length) {
            String remainingPath = String.join(".", java.util.Arrays.copyOfRange(paths, j + 1, paths.length));
            if (!remainingPath.isEmpty()) {
                DocumentContext elementContext = JsonPath.parse("{}");
                createPath(elementContext, "$." + remainingPath);
                element.putAll(elementContext.read("$"));
            }
        }

        return element;
    }

    /**
     * 将对象转换为数组的第一个元素，并扩展数组到目标大小
     */
    private static List<Object> convertObjectToArray(Object existingObject, int arraySize, boolean isLastBracket, int j, String[] paths) {
        List<Object> newArray = new ArrayList<>();
        newArray.add(existingObject); // 添加已有对象作为第一个元素

        for (int idx = 1; idx < arraySize; idx++) {
            newArray.add(createNestedElement(isLastBracket, j, paths));
        }

        return newArray;
    }

    // ==================== 辅助方法 ====================

    /**
     * 追加通配符到 parentPath
     */
    private static String appendWildcard(String parentPath, String pathName) {
        String result = parentPath;
        if (ObjectUtil.isNotEmpty(pathName)) {
            result += "." + pathName;
        }
        return result + "[*]";
    }

    /**
     * 追加引号键名到 parentPath
     */
    private static String appendQuotedKey(String parentPath, String pathName, String flag) {
        String nextKey = flag.substring(1, flag.length() - 1);
        if (ObjectUtil.isNotEmpty(pathName)) {
            return parentPath + "." + pathName + "." + nextKey;
        }
        return parentPath + "." + nextKey;
    }

    /**
     * 追加方括号到 parentPath
     */
    private static String appendBracket(String parentPath, String pathName, String flag) {
        if (ObjectUtil.isNotEmpty(pathName)) {
            return parentPath + "." + pathName + "[" + flag + "]";
        }
        return parentPath + "[" + flag + "]";
    }

    /**
     * 追加到 parentPath
     */
    private static String appendToParentPath(String parentPath, String path) {
        if ("*".equals(path)) {
            return parentPath + "[*]";
        }
        return parentPath + "." + path;
    }

    /**
     * 获取 JSON 值，失败返回 null
     */
    public static <T> T getJsonValue(DocumentContext jsonDocument, String jsonPath, Class<T> clazz) {
        try {
            return jsonDocument.read(jsonPath, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查路径段中从指定位置之后是否还有后续的方括号索引
     */
    private static boolean hasMoreBrackets(String path, int afterIndex) {
        if (afterIndex >= path.length()) {
            return false;
        }
        return path.substring(afterIndex).contains("[");
    }

    /**
     * 安全地解析负数索引路径。如果路径包含通配符 [*]，则直接返回原路径
     */
    private static String safeResolveNegativeIndices(DocumentContext context, String path) {
        if (path.contains("[*]")) {
            return path;
        }
        return resolveNegativeIndices(context, path);
    }

    // ==================== 负数索引解析 ====================

    /**
     * 将包含负数下标的路径转换为正数下标的路径
     */
    public static String resolveNegativeIndices(DocumentContext context, String originalPath) {
        String resolvedPath = originalPath;
        Matcher matcher = NEGATIVE_INDEX_PATTERN.matcher(resolvedPath);

        while (matcher.find()) {
            String negativeIndexStr = matcher.group(1);
            int negativeIndex = Integer.parseInt(negativeIndexStr);

            String arrayPath = resolvedPath.substring(0, matcher.start());
            Object arrayObj = context.read(arrayPath);

            int size = 0;
            if (arrayObj instanceof List) {
                size = ((List<?>) arrayObj).size();
            } else {
                throw new IllegalArgumentException("路径 " + arrayPath + " 不是数组");
            }

            int positiveIndex = size + negativeIndex;
            if (positiveIndex < 0) {
                throw new IndexOutOfBoundsException("负数下标越界: " + negativeIndex + ", 数组长度: " + size);
            }

            String replacement = "[" + positiveIndex + "]";
            resolvedPath = resolvedPath.substring(0, matcher.start()) + replacement + resolvedPath.substring(matcher.end());
            matcher = NEGATIVE_INDEX_PATTERN.matcher(resolvedPath);
            break;
        }

        if (NEGATIVE_INDEX_PATTERN.matcher(resolvedPath).find()) {
            return resolveNegativeIndices(context, resolvedPath);
        }

        return resolvedPath;
    }

    // ==================== 范围解析 ====================

    /**
     * 解析范围参数，支持格式：[start:end], [start:], [:end], [::step], [start:end:step]
     */
    private static RangeParams parseRangeParams(String flag) {
        String rangeStr = flag;
        if (flag.startsWith("[") && flag.endsWith("]")) {
            rangeStr = flag.substring(1, flag.length() - 1);
        }

        String[] parts = rangeStr.split(":", -1);

        Integer start = parts.length >= 1 ? parseRangeValue(parts[0]) : null;
        Integer end = parts.length >= 2 ? parseRangeValue(parts[1]) : null;
        Integer step = parts.length >= 3 ? parseRangeValue(parts[2]) : null;

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
     * 根据范围参数确定需要生成的对象数量
     */
    private static int determineMaxSize(RangeParams params) {
        int maxSize = 1;

        if (params.hasStart() && params.hasEnd()) {
            int start = params.getStart() != null ? params.getStart() : 0;
            int end = params.getEnd() != null ? params.getEnd() : 0;

            if (start < 0 || end < 0) {
                maxSize = Math.max(Math.abs(start), Math.abs(end));
            } else {
                maxSize = Math.max(start, end);
            }
        } else if (params.hasStart() && !params.hasEnd()) {
            int start = params.getStart() != null ? params.getStart() : 0;
            maxSize = start < 0 ? Math.abs(start) : start;
        } else if (!params.hasStart() && params.hasEnd()) {
            int end = params.getEnd() != null ? params.getEnd() : 0;
            maxSize = end < 0 ? Math.abs(end) : end;
        } else if (params.hasStep()) {
            int step = params.getStep() != null ? Math.abs(params.getStep()) : 1;
            maxSize = step;
        }

        return Math.max(1, maxSize);
    }

    // ==================== 范围参数类 ====================

    /**
     * 范围参数封装
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

    // ==================== 测试入口 ====================

    public static void main(String[] args) throws Exception {
        testCase("空对象普通路径", "{}", "a.b.c", "{\"a\":{\"b\":{\"c\":{}}}}");
        testCase("通配符 [*]", "{}", "$.users[*].name", "{\"users\":[{\"name\":{}}]}");
        testCase("通配符 *", "{}", "$.users.*.b.c", "{\"users\":[{\"b\":{\"c\":{}}}]}");
        testCase("连续数组索引", "{}", "name[2][2][\"n2\"][*].a.b.c", "{\"name\":[{},{},[{},{},{\"n2\":[{\"a\":{\"b\":{\"c\":{}}}}]}]]}");
        testCase("范围索引", "{}", "$.a[0:2].b[0:3].c", "{\"a\":[{\"b\":[{\"c\":{}},{\"c\":{}},{\"c\":{}}]},{\"b\":[{\"c\":{}},{\"c\":{}},{\"c\":{}}]}]}");
        testCase("负数索引", "{\"users\":[{},{}]}", "$.users[-1].name", "{\"users\":[{}, {\"name\":{}}]}");
        testCase("负数索引2", "{}", "$.users[-1].name", "{\"users\":[{\"name\":{}}]}");
        testCase("引号键名", "{}", "$[\"b\"][\"name\"]", "{\"b\":{\"name\":{}}}");
        testCase("已存在数组扩展", "{\"name\":[{},{},{}]}", "name[5].b", "{\"name\":[{},{},{},{},{},{\"b\":{}}]}");

        System.out.println("\n===== 所有测试完成 =====");
    }

    private static void testCase(String name, String json, String jsonPath, String expected) {
        System.out.println("\n--- 测试: " + name + " ---");
        System.out.println("JSON: " + json);
        System.out.println("路径: " + jsonPath);
        try {
            DocumentContext parse = JsonPath.parse(json);
            createPath(parse, jsonPath);
            String result = parse.jsonString();
            System.out.println("结果: " + result);
            if (expected != null) {
                boolean match = result.replace(" ", "").equals(expected.replace(" ", ""));
                System.out.println("预期: " + expected + " => " + (match ? "✓ 通过" : "✗ 失败"));
            }
        } catch (Exception e) {
            System.out.println("✗ 异常: " + e.getMessage());
        }
    }
}
