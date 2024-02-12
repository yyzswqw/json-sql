package json.sql.udf;

import cn.hutool.core.util.ObjectUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

import java.util.*;

public class InnerUdfMethod {

    /**
     * 如果指定的jsonPath的值为空，则删除指定的jsonPath，jsonPath为空则默认删除全部
     * @param curDocumentContext 当前写DocumentContext
     * @param jsonPaths jsonPath，为空则默认删除全部
     * @return 成功删除的jsonPath个数
     */
    public static Integer delIfNull(DocumentContext curDocumentContext, String ... jsonPaths) {
        if(ObjectUtil.isEmpty(jsonPaths)){
            jsonPaths = new String[]{"$.*"};
        }
        int result = 0;
        for (String jsonPath : jsonPaths) {
            try {
                Object read = curDocumentContext.read(jsonPath);
                if(Objects.isNull(read)){
                    curDocumentContext.delete(jsonPath);
                    result += 1;
                }
            }catch (Exception e){}
        }
        return result;
    }

    /**
     * 删除指定的jsonPath，jsonPath为空则默认删除全部
     * @param curDocumentContext 当前写DocumentContext
     * @param jsonPaths jsonPath，为空则默认删除全部
     * @return 成功删除的jsonPath个数
     */
    public static Integer del(DocumentContext curDocumentContext, String ... jsonPaths) {
        if(ObjectUtil.isEmpty(jsonPaths)){
            jsonPaths = new String[]{"$.*"};
        }
        int result = 0;
        for (String jsonPath : jsonPaths) {
            try {
                curDocumentContext.delete(jsonPath);
                result += 1;
            }catch (Exception e){}
        }
        return result;
    }

    public static Object formatAllLevel(DocumentContext curDocumentContext, String jsonPath, String ... ignoreKeys) {
        return format(curDocumentContext, jsonPath, Long.MAX_VALUE, ignoreKeys);
    }

    public static Object format(DocumentContext curDocumentContext, String jsonPath, Long level, String ... ignoreKeys) {
        String json = curDocumentContext.jsonString();
        DocumentContext parse = JsonPath.parse(json);
        if(ObjectUtil.isEmpty(jsonPath)){
            jsonPath = "$";
        }
        if(ObjectUtil.isEmpty(level)){
            level = Long.MAX_VALUE;
        }
        innerFormat(parse, jsonPath, level, ignoreKeys);
        Object read = null;
        try {
            read = parse.read(jsonPath);
        }catch (Exception e){
            return null;
        }
        if(read instanceof Map){
            Map<Object,Object> valMap = (Map) read;
            for (Map.Entry<Object, Object> entry : valMap.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                curDocumentContext.put(jsonPath,key.toString(),value);
            }
        }else{
            curDocumentContext.set(jsonPath, read);
        }
        return read;
    }

    /**
     * 格式化json中的字符串json,将字符串的json转换为一个正常的json，jsonPath为空则默认为根路径
     * @param curDocumentContext 当前写DocumentContext
     * @param jsonPath jsonPath，为空则默认为根路径
     * @param level 转换的最大层级
     * @param ignoreKeys 忽略的key
     * @return curDocumentContext下的所有内容
     */
    public static Object innerFormat(DocumentContext curDocumentContext, String jsonPath, Long level, String ... ignoreKeys) {
        if(ObjectUtil.isEmpty(level) || level < 0){
            return curDocumentContext.json();
        }
        if(ObjectUtil.isEmpty(jsonPath)){
            jsonPath = "$";
        }
        Set<String> ignoreKeySet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreKeys)){
            ignoreKeySet.addAll(Arrays.asList(ignoreKeys));
        }
        Object read = null;
        try {
            read = curDocumentContext.read(jsonPath);
            if(ObjectUtil.isEmpty(read)){
                return curDocumentContext.json();
            }
        }catch (Exception e){
            return curDocumentContext.json();
        }
        if(read instanceof JSONArray){
            JSONArray readValue = (JSONArray)read;
            int tempSize = readValue.size();
            List<Integer> removeIndexList = new ArrayList<>();
            List<Object> setList = new ArrayList<>();
            for (int i = 0; i < tempSize; i++) {
                Object val = readValue.get(i);
                try {
                    DocumentContext parse = JsonPath.parse(val);
                    // 数组下每个对象的层级应该和父级相同
                    Object values = innerFormat(parse,"$",level,ignoreKeys);
                    if(ObjectUtil.isNotEmpty(values)){
                        curDocumentContext.set(jsonPath+String.format("[%d]",i ),values);
                    }else {
                        removeIndexList.add(i);
                        setList.add(val);
                    }
                } catch (Exception e) {
                    removeIndexList.add(i);
                    setList.add(val);
                }
            }
            for (int i = 0; i < removeIndexList.size(); i++) {
                Integer index = removeIndexList.get(i);
                curDocumentContext.set(jsonPath+String.format("[%d]",index),setList.get(i));
            }
        }else {
            if(read instanceof CharSequence){
                DocumentContext parse = null;
                try {
                    parse = JsonPath.parse(read.toString());
                }catch (Exception e){
                    // 无法转json
                }
                if (!(parse.json() instanceof CharSequence)) {
                    // 单纯是一个字符，不是一个json,如果是一个正常的json,经过上一步后应该是一个jsonArray 或者 jsonObject(Map)
                    if(ObjectUtil.isNotEmpty(parse)){
                        Object values = innerFormat(parse,"$",level - 1,ignoreKeys);
//                    curDocumentContext = parse;
                        if(ObjectUtil.isNotEmpty(values)){
                            curDocumentContext = parse;
                        }
                    }
                }

            }else{
                Map<Object,Object> valueMap = new LinkedHashMap<>();
                try {
                    valueMap = curDocumentContext.read(jsonPath, Map.class);
                }catch (Exception e){
                    // 无法转map,不能打平展开,如：基础类型的数组，["red",19.95,1,true,"qwq",1234,2,false,"qwq1"]
                }

                Set<Map.Entry<Object, Object>> entries = valueMap.entrySet();
                for (Map.Entry<Object, Object> entry : entries) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if(ignoreKeySet.contains(key) || ObjectUtil.isEmpty(value)){
                        curDocumentContext.put(jsonPath,key.toString(),value);
                        continue;
                    }
                    try {
                        DocumentContext parse = JsonPath.parse(value);
                        // 能解析说明可以递归
                        if(level - 1 < 0){
                            return curDocumentContext.json();
                        }else {
                            Object values = innerFormat(parse,"$",level - 1,ignoreKeys);
                            if(ObjectUtil.isNotEmpty(values)){
                                curDocumentContext.put(jsonPath,key.toString(),values);
                            }
                        }
                    } catch (Exception e) {
                        curDocumentContext.put(jsonPath,key.toString(),value);
                    }
                }
            }
        }
        return curDocumentContext.json();
    }


    /**
     * 返回数 组的 size,如果是对象，返回的是一级 key 的 个数，jsonPath为空则默认为根路径
     * @param curDocumentContext 当前写DocumentContext
     * @param jsonPath jsonPath，为空则默认为根路径
     * @param objReturnSize 如果不是数组，是json对象，则是否返回json对象的一级key的个数，默认不返回
     * @return size
     */
    public static Long size(DocumentContext curDocumentContext, String jsonPath,Boolean objReturnSize) {
        Object read = null;
        if(ObjectUtil.isEmpty(jsonPath)){
            jsonPath = "$";
        }
        try {
            read = curDocumentContext.read(jsonPath);
            if(ObjectUtil.isEmpty(read)){
                return 0L;
            }
        }catch (Exception e){
            return 0L;
        }
        if(read instanceof JSONArray){
            JSONArray readValue = (JSONArray)read;
            return Long.parseLong(String.valueOf(readValue.size())) ;
        }else {
            try {
                Collection valueCollection = curDocumentContext.read(jsonPath, Collection.class);
                return Long.parseLong(String.valueOf(valueCollection.size()));
            }catch (Exception e){}

            if(ObjectUtil.isNotEmpty(objReturnSize) && objReturnSize){
                try {
                    Map valueMap = curDocumentContext.read(jsonPath, Map.class);
                    return Long.parseLong(String.valueOf(valueMap.size()));
                }catch (Exception e1){
                    return 0L;
                }
            }
            return 0L;
        }
    }

    public static Set<Object> values(DocumentContext curDocumentContext, String jsonPath, String ... ignoreKeys) {
        return innerValues(curDocumentContext,jsonPath,Long.MAX_VALUE,ignoreKeys);
    }

    public static Set<Object> valuesByLevel(DocumentContext curDocumentContext, String jsonPath,Long level, String ... ignoreKeys) {
        return innerValues(curDocumentContext,jsonPath,level,ignoreKeys);
    }

    /**
     * 获取jsonPath下的所有的value，jsonPath为空则默认为根路径
     * @param curDocumentContext 当前写DocumentContext
     * @param jsonPath root jsonpath，为空则默认为根路径
     * @param level 获取value的最大层级
     * @param ignoreKeys 忽略的key
     * @return 所有的value
     */
    public static Set<Object> innerValues(DocumentContext curDocumentContext, String jsonPath, Long level, String ... ignoreKeys) {
        Set<Object> result = new LinkedHashSet<>();
        if(ObjectUtil.isEmpty(level) || level < 0){
            return result;
        }
        Set<String> ignoreKeySet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreKeys)){
            ignoreKeySet.addAll(Arrays.asList(ignoreKeys));
        }
        Object read = null;
        if(ObjectUtil.isEmpty(jsonPath)){
            jsonPath = "$";
        }
        try {
            read = curDocumentContext.read(jsonPath);
            if(ObjectUtil.isEmpty(read)){
                return result;
            }
        }catch (Exception e){
            return result;
        }
        if(read instanceof JSONArray){
            JSONArray readValue = (JSONArray)read;
            for (int i = 0; i < readValue.size(); i++) {
                Object val = readValue.get(i);
                try {
                    DocumentContext parse = JsonPath.parse(val);
                    // 数组下每个对象的层级应该和父级相同
                    Set<Object> values = innerValues(parse,"$",level,ignoreKeys);
                    if(ObjectUtil.isNotEmpty(values)){
                        result.addAll(values);
                    }
                } catch (Exception e) {}
            }
        }else {
            Map<Object,Object> valueMap = new LinkedHashMap<>();
            try {
                valueMap = curDocumentContext.read(jsonPath, Map.class);
            }catch (Exception e){
                // 无法转map,不能打平展开,如：基础类型的数组，["red",19.95,1,true,"qwq",1234,2,false,"qwq1"]
                if(read instanceof Collection){
                    Collection<Object> collection = (Collection<Object>) read;
                    result.addAll(collection);
                }else{
                    result.add(read);
                }
            }

            Set<Map.Entry<Object, Object>> entries = valueMap.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if(ignoreKeySet.contains(key) || ObjectUtil.isEmpty(value)){
                    continue;
                }
                try {
                    DocumentContext parse = JsonPath.parse(value);
                    // 能解析说明可以递归
                    if(level - 1 < 0){
                        result.add(value);
                    }else {
                        Set<Object> values = innerValues(parse,"$",level - 1,ignoreKeys);
                        if(ObjectUtil.isNotEmpty(values)){
                            result.addAll(values);
                        }else{
                            result.add(value);
                        }
                    }
                } catch (Exception e) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    public static Set<Object> keys(DocumentContext curDocumentContext, String jsonPath){
        return innerKeys(curDocumentContext,jsonPath,Long.MAX_VALUE);
    }

    public static Set<Object> keysByLevel(DocumentContext curDocumentContext, String jsonPath,Long level){
        return innerKeys(curDocumentContext,jsonPath,level);
    }

    /**
     * 获取jsonPath下的所有的key，jsonPath为空则默认为根路径
     * @param curDocumentContext 当前写DocumentContext
     * @param jsonPath root jsonpath，为空则默认为根路径
     * @param level 获取key的最大层级
     * @return 所有key
     */
    public static Set<Object> innerKeys(DocumentContext curDocumentContext, String jsonPath, Long level) {
        Set<Object> result = new LinkedHashSet<>();
        if(ObjectUtil.isEmpty(level) || level < 0){
            return result;
        }
        Object read = null;
        if(ObjectUtil.isEmpty(jsonPath)){
            jsonPath = "$";
        }
        try {
            read = curDocumentContext.read(jsonPath);
            if(ObjectUtil.isEmpty(read)){
                return result;
            }
        }catch (Exception e){
            return result;
        }
        if(read instanceof JSONArray){
            JSONArray readValue = (JSONArray)read;
            for (int i = 0; i < readValue.size(); i++) {
                Object val = readValue.get(i);
                try {
                    DocumentContext parse = JsonPath.parse(val);
                    // 数组下每个对象的层级应该和父级相同
                    Set<Object> keys = innerKeys(parse,"$",level);
                    if(ObjectUtil.isNotEmpty(keys)){
                        result.addAll(keys);
                    }
                } catch (Exception e) {}
            }
        }else {
            Map<Object,Object> valueMap = new LinkedHashMap<>();
            try {
                valueMap = curDocumentContext.read(jsonPath, Map.class);
            }catch (Exception e){
                // 无法转map,不能打平展开,如：基础类型的数组，["red",19.95,1,true,"qwq",1234,2,false,"qwq1"]
            }
            Set<Map.Entry<Object, Object>> entries = valueMap.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                result.add(key);
                if(ObjectUtil.isEmpty(value)){
                    continue;
                }
                try {
                    DocumentContext parse = JsonPath.parse(value);
                    // 能解析说明可以递归
                    if(level - 1 >= 0){
                        Set<Object> keys = innerKeys(parse,"$",level - 1);
                        if(ObjectUtil.isNotEmpty(keys)){
                            result.addAll(keys);
                        }
                    }
                } catch (Exception e) {}
            }
        }
        return result;
    }

    public static Map<Object,Object> explode2(DocumentContext curDocumentContext, String jsonPath,Long level, String ... ignoreKeys){
        return innerExplode(curDocumentContext,null,true,jsonPath,true,false,false,level,ignoreKeys);
    }

    public static Map<Object,Object> explode3(DocumentContext curDocumentContext, String jsonPath, String ... ignoreKeys){
        return innerExplode(curDocumentContext,null,true,jsonPath,true,false,false,Long.MAX_VALUE,ignoreKeys);
    }

    public static Map<Object,Object> explode(DocumentContext curDocumentContext,
                                             Boolean putNewValue2TarJsonPath,
                                             String jsonPath,Boolean delOldJsonPath,
                                             Boolean arrayExplode,Boolean arrayExplodeConcatIndex,
                                             Long level,String ... ignoreKeys){
        return innerExplode(curDocumentContext,null,putNewValue2TarJsonPath,jsonPath,delOldJsonPath,arrayExplode,arrayExplodeConcatIndex,level,ignoreKeys);
    }

    public static Map<Object,Object> explodeAllLevel2(DocumentContext curDocumentContext,
                                                     Boolean putNewValue2TarJsonPath,
                                                     String jsonPath,Boolean delOldJsonPath,
                                                     Boolean arrayExplode,Boolean arrayExplodeConcatIndex,
                                                     String ... ignoreKeys){
        return explodeAllLevel(curDocumentContext,null,putNewValue2TarJsonPath,jsonPath,delOldJsonPath,arrayExplode,arrayExplodeConcatIndex,ignoreKeys);
    }

    public static Map<Object,Object> explodeAllLevel(DocumentContext curDocumentContext,
                                                     String tarJsonPath, Boolean putNewValue2TarJsonPath,
                                                     String jsonPath,Boolean delOldJsonPath,
                                                     Boolean arrayExplode,Boolean arrayExplodeConcatIndex,
                                                     String ... ignoreKeys){
        return innerExplode(curDocumentContext,tarJsonPath,putNewValue2TarJsonPath,jsonPath,delOldJsonPath,arrayExplode,arrayExplodeConcatIndex,Long.MAX_VALUE,ignoreKeys);
    }

    /**
     * 将json 对象打平展开，jsonPath为空则默认为根路径
     * @param curDocumentContext 当前写DocumentContext
     * @param tarJsonPath 目标jsonPath
     * @param putNewValue2TarJsonPath 是否需要将打平展开后的值放入 目标jsonPath
     * @param jsonPath 需要打平展开的jsonPath，为空则默认为根路径
     * @param delOldJsonPath 是否删除打平展开前的jsonPath
     * @param arrayExplode 数组是否打平展开
     * @param arrayExplodeConcatIndex 数组打平展开时，key是否拼接下标，不拼接的话，同名key会被替换
     * @param level 打平展开的最大层级
     * @param ignoreKeys 忽略打平展开的key
     * @return 打平展开后的值
     */
    public static Map<Object,Object> innerExplode(DocumentContext curDocumentContext,
                                                  String tarJsonPath, Boolean putNewValue2TarJsonPath,
                                                  String jsonPath,Boolean delOldJsonPath,
                                                  Boolean arrayExplode,Boolean arrayExplodeConcatIndex,
                                                  Long level, String ... ignoreKeys){
        Map<Object,Object> resultMap = new HashMap<>();
        Set<String> ignoreKeySet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreKeys)){
            ignoreKeySet.addAll(Arrays.asList(ignoreKeys));
        }
        if(ObjectUtil.isEmpty(level) || level < 0){
            return resultMap;
        }
        Object read = null;
        if(ObjectUtil.isEmpty(jsonPath)){
            jsonPath = "$";
        }
        try {
            read = curDocumentContext.read(jsonPath);
            if(ObjectUtil.isEmpty(read)){
                return resultMap;
            }
        }catch (Exception e){
            return resultMap;
        }
        if(read instanceof JSONArray){
            JSONArray readValue = (JSONArray)read;
            String path = "$";
            String name = null;
            if(ObjectUtil.isNotEmpty(jsonPath)){
                int i = jsonPath.lastIndexOf(".");
                if(i >= 0){
                    path = jsonPath.substring(0,i);
                    // 名称至少要有一位，必须小于length
                    if(i+1 < jsonPath.length()){
                        name = jsonPath.substring(i+1);
                    }
                }
            }
            if(ObjectUtil.isNotEmpty(arrayExplode) && arrayExplode){
                for (int i = 0; i < readValue.size(); i++) {
                    Object val = readValue.get(i);
                    String tempName = name;
                    if(ObjectUtil.isNotEmpty(name)){
                        tempName += "_" + i;
                    }
                    try {
                        DocumentContext parse = JsonPath.parse(val);
                        // 能解析说明可以递归
                        if(level - 1 <= 0){
                            if(ObjectUtil.isNotEmpty(tempName)){
                                // 如果名称为空，说明jsonpath 是$,不做处理
                                if(!ignoreKeySet.contains(name)){
                                    // tempName是由name衍生出来的，如果name被忽略，那它的衍生名也忽略
                                    resultMap.put(tempName,val);
                                }
                            }
                        }else{
                            // 数组下每个对象的层级应该和父级相同
                            Map<Object, Object> valMap = innerExplode(parse, null,false,"$", false,arrayExplode,arrayExplodeConcatIndex,level, ignoreKeys);
                            if(ObjectUtil.isNotEmpty(valMap)){
                                for (Map.Entry<Object, Object> objectObjectEntry : valMap.entrySet()) {
                                    Object key = objectObjectEntry.getKey();
                                    Object value = objectObjectEntry.getValue();
                                    if(ObjectUtil.isNotEmpty(arrayExplodeConcatIndex) && arrayExplodeConcatIndex){
                                        resultMap.put(key.toString()+"_"+i,value);
                                    }else{
                                        resultMap.put(key,value);
                                    }
                                }
                            }else{
                                if(ObjectUtil.isNotEmpty(tempName)){
                                    // 如果名称为空，说明jsonpath 是$,不做处理
                                    if(!ignoreKeySet.contains(name)){
                                        // tempName是由name衍生出来的，如果name被忽略，那它的衍生名也忽略
                                        resultMap.put(tempName,val);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if(ObjectUtil.isNotEmpty(tempName)){
                            // 如果名称为空，说明jsonpath 是$,不做处理
                            if(!ignoreKeySet.contains(name)){
                                // tempName是由name衍生出来的，如果name被忽略，那它的衍生名也忽略
                                resultMap.put(tempName,val);
                            }
                        }
                    }
                }
            }else{
                if(ObjectUtil.isNotEmpty(name)){
                    // 如果名称为空，说明jsonpath 是$,不做处理
                    if(!ignoreKeySet.contains(name)){
                        resultMap.put(name,readValue);
                    }
                }
            }
        }else{
            Map<Object,Object> valueMap = new LinkedHashMap<>();
            try {
                valueMap = curDocumentContext.read(jsonPath, Map.class);
            }catch (Exception e){
                // 无法转map,不能打平展开,如：基础类型的数组，["red",19.95,1,true,"qwq",1234,2,false,"qwq1"]
            }
            Set<Map.Entry<Object, Object>> entries = valueMap.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if(ignoreKeySet.contains(key)){
                    continue;
                }
                if(ObjectUtil.isEmpty(value)){
                    resultMap.put(key,null);
                    continue;
                }
                try {
                    DocumentContext parse = JsonPath.parse(value);
                    // 能解析说明可以递归
                    if(level - 1 < 0){
                        resultMap.put(key,value);
                    }else{
                        Map<Object, Object> valMap = innerExplode(parse, null,false,"$", false,arrayExplode,arrayExplodeConcatIndex,level - 1, ignoreKeys);
                        if(ObjectUtil.isNotEmpty(valMap)){
                            resultMap.putAll(valMap);
                        }else{
                            resultMap.put(key,value);
                        }
                    }
                } catch (Exception e) {
                    resultMap.put(key,value);
                }
            }
            String path = "$";
            String name = null;
            if(ObjectUtil.isNotEmpty(tarJsonPath)){
                int i = tarJsonPath.lastIndexOf(".");
                if(i >= 0){
                    path = tarJsonPath.substring(0,i);
                    // 名称至少要有一位，必须小于length
                    if(i+1 < tarJsonPath.length()){
                        name = tarJsonPath.substring(i+1);
                    }
                }
            }
            if(ObjectUtil.isNotEmpty(putNewValue2TarJsonPath) && putNewValue2TarJsonPath){
                if(ObjectUtil.isNotEmpty(name)){
                    curDocumentContext.put(path,name,resultMap);
                }else {
                    for (Map.Entry<Object, Object> objectObjectEntry : resultMap.entrySet()) {
                        Object key = objectObjectEntry.getKey();
                        Object value = objectObjectEntry.getValue();
                        curDocumentContext.put(path,key.toString(),value);
                    }
                }
            }
        }
        if(ObjectUtil.isNotEmpty(delOldJsonPath) && delOldJsonPath){
            curDocumentContext.delete(jsonPath);
        }
        return resultMap;
    }

}
