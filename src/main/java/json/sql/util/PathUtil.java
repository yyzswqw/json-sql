package json.sql.util;

import cn.hutool.core.util.ObjectUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PathUtil {

    private PathUtil(){}

    public static void main(String[] args) throws Exception {
        String json = "{}";
//        String json = "{\"name\":[{},{},[{},{},{\"n2\":[{},{},{},{},{}]}]]}";
//        String jsonPath = "name[2][2][\"n2\"][*].a.b.c";
        String jsonPath = "$.name[2][2][\"n2\"][0:1]['b']['c']";
        DocumentContext parse = JsonPath.parse(json);
        createPath(parse, jsonPath);
        System.out.println(parse.jsonString());
    }

    public static void createPath(DocumentContext documentContext,String jsonPath){
        String[] paths = jsonPath.split("\\.");
        String parentPath = "$";
        for (int j = 0; j < paths.length; j++) {
            String path = paths[j];
            if("$".equals(path)){
                continue;
            }
            // $..name,这种写法就会是空
            if(ObjectUtil.isEmpty(path)){
                continue;
            }
            Object jsonValue = getJsonValue(documentContext, parentPath+"."+path, Object.class);
            if(ObjectUtil.isEmpty(jsonValue)){
                if (path.contains("[")) {
                    int startFlag = 0;
                    int firstFlagIndex = 0;
                    while (path.indexOf("[",startFlag) >= 0){
                        path = path.trim();
                        String pathName = path.substring(startFlag, path.indexOf("[",startFlag));
                        firstFlagIndex = path.indexOf("]", firstFlagIndex+1);
                        String flag = path.substring(path.indexOf("[",startFlag) + 1, firstFlagIndex);
                        if("*".equals(flag)){
                            if(ObjectUtil.isNotEmpty(pathName)){
                                Object value = getJsonValue(documentContext, parentPath+"."+pathName, Object.class);
                                if(ObjectUtil.isEmpty(value)){
                                    documentContext.put(parentPath,pathName, new HashMap<>());
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
                                    documentContext.put(parentPath,pathName, new HashMap<>());
                                }
                                createPath(documentContext,parentPath + "."+pathName+"."+nextKey);
                                parentPath += "."+pathName+"."+nextKey;
                            }
                        }else if(flag.contains(":")){
                            // 范围的情况，不存在时，只建一个子对象，存在时，会对满足条件下的子对象都新增上对应的key。如 ：
                            // String json = "{\"name\":[{},{},[{},{},{\"n2\":[{},{},{},{},{}]}]]}";
                            //  String jsonPath = "$.name[2][2][\"n2\"][0:4]['b']['c']";
                            List<Object> list = new ArrayList<>();
                            list.add(new HashMap<>());
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
                            int i = Integer.parseInt(flag);
                            List<Object> list = new ArrayList<>();
                            for (int i1 = 0; i1 < i+1; i1++) {
                                list.add(new HashMap<>());
                            }
                            // 负数的情况
                            if(list.isEmpty()){
                                list.add(new HashMap<>());
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
                        documentContext.put(parentPath,path, new HashMap<>());
                    }
                    parentPath += "."+path;
                }

            }else{
                parentPath += "."+path;
            }
        }
    }

    public static <T> T getJsonValue(DocumentContext jsonDocument, String jasonPath, Class<T> clazz) {
        try {
            return jsonDocument.read( jasonPath,clazz);
        }catch (Exception e){
            return null;
        }
    }

}
