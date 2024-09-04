package json.shell.annotation;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import json.shell.ShellContext;
import json.shell.entity.CommandDescInfo;
import json.shell.entity.CommandParamDescInfo;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.*;

@Slf4j
public class CommandParser {

    private CommandParser(){

    }

    /**
     * 注册 class udf 函数
     * @param clazz class
     * @param onlyParseAnnotation 是否只解析带有注解的函数
     * @param ignoreCommandName 需要忽略的命令
     */
    public static List<CommandDescInfo> classParser(Class<?> clazz, boolean onlyParseAnnotation, String ... ignoreCommandName){
        List<CommandDescInfo> result = new ArrayList<>();
        List<Method> allPublicMethodList = getAllPublicMethod(clazz);
        Set<String> ignoreCommandNameSet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreCommandName)){
            ignoreCommandNameSet.addAll(Arrays.asList(ignoreCommandName));
        }
        for (Method method : allPublicMethodList) {
            CommandMethodIgnore commandMethodIgnore = method.getAnnotation(CommandMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(commandMethodIgnore)){
                continue;
            }
            List<String> commandNames = new ArrayList<>();
            String commandName = method.getName();
            CommandMethod commandMethod = method.getAnnotation(CommandMethod.class);
            if(onlyParseAnnotation && ObjectUtil.isEmpty(commandMethod)){
                continue;
            }
            if(ObjectUtil.isNotEmpty(commandMethod) && ObjectUtil.isNotEmpty(commandMethod.name())){
                commandNames = Arrays.asList(commandMethod.name());
            }
            if(ObjectUtil.isEmpty(commandNames)){
                commandNames.add(commandName);
            }
            try {
                List<CommandDescInfo> commandDescInfos = getCommandDescInfo(clazz, method);
                if(ObjectUtil.isNotEmpty(commandDescInfos)){
                    commandDescInfos.removeIf(commandDescInfo -> ignoreCommandNameSet.contains(commandDescInfo.getName()));
                }
                if(ObjectUtil.isNotEmpty(commandDescInfos)){
                    result.addAll(commandDescInfos);
                }
            }catch (Exception e){
                if (log.isDebugEnabled()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    log.debug("注册 command 失败! commandNames : {} ,class : {} ,method : {} ,parameterTypes : {}",commandNames,clazz.getName(),method.getName(),parameterTypes);
                    log.debug("注册 command 失败!",e);
                }
            }
        }
        return result;
    }

    /**
     * 注册 class udf 函数
     * @param clazz class
     * @param onlyParseAnnotation 是否只解析带有注解的函数
     * @param ignoreMethod 需要忽略的方法
     */
    public static List<CommandDescInfo> classParser(Class<?> clazz,boolean onlyParseAnnotation,Method ... ignoreMethod){
        List<CommandDescInfo> result = new ArrayList<>();
        List<Method> allPublicStaticMethodList = getAllPublicMethod(clazz);
        Set<Method> ignoreCommandMethodSet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreMethod)){
            ignoreCommandMethodSet.addAll(Arrays.asList(ignoreMethod));
        }

        for (Method method : allPublicStaticMethodList) {
            CommandMethodIgnore commandMethodIgnore = method.getAnnotation(CommandMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(commandMethodIgnore)){
                continue;
            }
            List<String> commandNames = new ArrayList<>();
            String commandName = method.getName();
            CommandMethod commandMethod = method.getAnnotation(CommandMethod.class);
            if(onlyParseAnnotation && ObjectUtil.isEmpty(commandMethod)){
                continue;
            }
            if(ObjectUtil.isNotEmpty(commandMethod) && ObjectUtil.isNotEmpty(commandMethod.name())){
                for (String s : commandMethod.name()) {
                    if(ObjectUtil.isNotEmpty(s) && ObjectUtil.isNotEmpty(s.trim())){
                        commandNames.add(s.trim());
                    }
                }
            }
            if(ObjectUtil.isEmpty(commandNames)){
                commandNames.add(commandName);
            }
            if(ignoreCommandMethodSet.contains(method)){
                continue;
            }
            try {
                List<CommandDescInfo> commandDescInfos = getCommandDescInfo(clazz, method);
                if(ObjectUtil.isNotEmpty(commandDescInfos)){
                    result.addAll(commandDescInfos);
                }
            }catch (Exception e){
                if (log.isDebugEnabled()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    log.debug("注册 command 失败! commandNames : {} ,class : {} ,method : {} ,parameterTypes : {}",commandNames,clazz.getName(),method.getName(),parameterTypes);
                    log.debug("注册 command 失败!",e);
                }
            }
        }
        return result;
    }

    /**
     * 获取一个udf 函数的描述信息
     * @param method udf函数
     * @return 描述信息
     */
    public static List<CommandDescInfo> getCommandDescInfo(Class<?> clazz,Method method){
        if(!checkCommandMethod(clazz,method)){
            return null;
        }
        Object instance = null;
        if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
            // 非静态方法，需要一个实例执行方法
            if(!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isEnum()){
                try {
                    Constructor<?> constructor = clazz.getConstructor();
                    boolean hasPublicNoArgConstructor = Modifier.isPublic(constructor.getModifiers()) &&
                            constructor.getParameterCount() == 0;
                    if(hasPublicNoArgConstructor){
                        instance = clazz.newInstance();
                    }
                } catch (Exception ignored) {}
            }
        }

        CommandDescInfo descInfo = new CommandDescInfo();
        descInfo.setInstance(instance);
        descInfo.setMethod(method);

        List<String> commandNames = new ArrayList<>();
        String commandName = method.getName();
        CommandMethod commandMethod = method.getAnnotation(CommandMethod.class);
        if(ObjectUtil.isNotEmpty(commandMethod) && ObjectUtil.isNotEmpty(commandMethod.name())){
            for (String s : commandMethod.name()) {
                if(ObjectUtil.isNotEmpty(s) && ObjectUtil.isNotEmpty(s.trim())){
                    commandNames.add(s.trim());
                }
            }
        }
        if(ObjectUtil.isEmpty(commandNames)){
            commandNames.add(commandName);
        }

        if(ObjectUtil.isNotEmpty(commandMethod) && ObjectUtil.isNotEmpty(commandMethod.desc())){
            descInfo.setFunctionDesc(commandMethod.desc());
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        if(ObjectUtil.isEmpty(parameters) || parameters.length < 1){
            throw new RuntimeException("command : " + commandNames + " must has one parameter ["+ ShellContext.class.getName() +"]");
        }
        Class<?> shellContextParameterType = parameterTypes[0];
        if( shellContextParameterType != ShellContext.class){
            throw new RuntimeException("command : " + commandNames + " first parameter is not ["+ ShellContext.class.getName() +"]");
        }

        Type[] genericParameterTypes = method.getGenericParameterTypes();
        for (int i = 1; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameterTypes[i];
            String paramName = parameter.getName();
            CommandParamDescInfo paramDescInfo = new CommandParamDescInfo();
            paramDescInfo.setParamType(parameterType.getSimpleName());
            paramDescInfo.setParamName(paramName);
            paramDescInfo.setArgsType(parameterTypes[i]);

            if(parameterType.isArray() || Map.class.isAssignableFrom(parameterType) || Collection.class.isAssignableFrom(parameterType)){
                List<String> simpleNameList = new ArrayList<>();
                Type genericParameterType = genericParameterTypes[i];
                List<Class<?>> genericityTypeList = new ArrayList<>();
                if (genericParameterType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericParameterType;
                    Type[] actualTypeArguments = paramType.getActualTypeArguments();
                    try {
                        for (Type actualTypeArgument : actualTypeArguments) {
                            Class<?> actualTypeArgumentClass = (Class<?>) actualTypeArgument;
                            String simpleName = actualTypeArgumentClass.getSimpleName();
                            simpleNameList.add(simpleName);
                            genericityTypeList.add(actualTypeArgumentClass);
                        }
                    }catch (Exception e){
                        simpleNameList.clear();
                        genericityTypeList.clear();
                    }
                }
                if(ObjectUtil.isNotEmpty(simpleNameList)){
                    paramDescInfo.setParamType(String.format("%s<%s>", paramDescInfo.getParamType(),String.join(",", simpleNameList)));
                    paramDescInfo.getGenericityTypeList().addAll(genericityTypeList);
                }else if(Map.class.isAssignableFrom(parameterType)){
                    paramDescInfo.setParamType(String.format("%s<%s,%s>", paramDescInfo.getParamType(),Object.class.getSimpleName(),Object.class.getSimpleName()));
                    paramDescInfo.getGenericityTypeList().addAll(Arrays.asList(Object.class,Object.class));
                }else if(Collection.class.isAssignableFrom(parameterType)){
                    paramDescInfo.setParamType(String.format("%s<%s>", paramDescInfo.getParamType(),Object.class.getSimpleName()));
                    paramDescInfo.getGenericityTypeList().add(Object.class);
                }
            }
            String paramDesc = "";
            CommandParam commandParam = parameter.getAnnotation(CommandParam.class);
            if(ObjectUtil.isNotEmpty(commandParam) && ObjectUtil.isNotEmpty(commandParam.desc())){
                paramDesc = commandParam.desc();
            }
            if(ObjectUtil.isNotEmpty(commandParam) && ObjectUtil.isNotEmpty(commandParam.name())){
                paramDescInfo.setParamName(commandParam.name());
            }
            paramDescInfo.setParamDesc(paramDesc);
            descInfo.getCommandParamDescInfoList().add(paramDescInfo);
        }
        String returnTypeSimpleName = method.getReturnType().getSimpleName();
        // 获取方法的返回类型
        Type returnType = method.getGenericReturnType();
        descInfo.setReturnType(returnTypeSimpleName);
        List<String> simpleNameList = new ArrayList<>();
        if (returnType instanceof ParameterizedType) {
            // 强制转换为参数化类型
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            // 获取泛型类型的数组
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            // 输出泛型类型
            for (Type typeArgument : typeArguments) {
                String simpleName = typeArgument.getTypeName();
                try {
                    simpleName = ((Class<?>) typeArgument).getSimpleName();
                }catch (Exception ignored){}
                simpleNameList.add(simpleName);
            }
        }
        if(ObjectUtil.isNotEmpty(simpleNameList)){
            descInfo.setReturnType(String.format("%s<%s>", descInfo.getReturnType(),String.join(",", simpleNameList)));
        }
        List<CommandDescInfo> commandDescInfoList = new ArrayList<>();
        Set<String> commandNameSet = new HashSet<>();
        for (String name : commandNames) {
            if (!commandNameSet.contains(name)) {
                CommandDescInfo descInfoTemp = new CommandDescInfo();
                BeanUtil.copyProperties(descInfo,descInfoTemp);
                descInfoTemp.setName(name);
                if(ObjectUtil.isEmpty(descInfoTemp.getFunctionDesc())){
                    descInfoTemp.setFunctionDesc(name);
                }
                if(ObjectUtil.isNotEmpty(descInfo.getCommandParamDescInfoList())){
                    descInfoTemp.getCommandParamDescInfoList().addAll(descInfo.getCommandParamDescInfoList());
                }
                commandDescInfoList.add(descInfoTemp);
                commandNameSet.add(name);
            }
        }
        return commandDescInfoList;
    }

    /**
     * 校验一个方法是否可以被注册为UDF
     * @param method method
     * @return true:是，false:否
     */
    public static boolean checkCommandMethod(Class<?> clazz,Method method) {
        if(ObjectUtil.isEmpty(method)){
            return false;
        }
        // 获取参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        if(!Modifier.isPublic(method.getModifiers())){
            if (log.isDebugEnabled()) {
                log.debug("check command method result : false . reason : {} , methodName : {} , parameterTypes : {}","is not a public method",method.getName(),parameterTypes);
            }
            return false;
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            // 非静态方法，却不能实例化
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()){
                if (log.isDebugEnabled()) {
                    log.debug("check command method result : false . reason : {} , methodName : {} , parameterTypes : {}","it is a static method but can not instantiate",method.getName(),parameterTypes);
                }
                return false;
            }
            // 能实例化，但没有公有无参构造方法
            try {
                Constructor<?> constructor = clazz.getConstructor();
                boolean hasPublicNoArgConstructor = Modifier.isPublic(constructor.getModifiers()) &&
                        constructor.getParameterCount() == 0;
                if(!hasPublicNoArgConstructor){
                    if (log.isDebugEnabled()) {
                        log.debug("check command method result : false . reason : {} , methodName : {} , parameterTypes : {}","it is not a static method but there is no public constructor without parameters",method.getName(),parameterTypes);
                    }
                    return false;
                }
            } catch (Exception ignored) {
                if (log.isDebugEnabled()) {
                    log.debug("check command method result : false . reason : {} , methodName : {} , parameterTypes : {}","it is a static method but there is no public constructor without parameters",method.getName(),parameterTypes);
                }
                return false;
            }
        }
        if(ObjectUtil.isEmpty(parameterTypes)){
            return true;
        }
        // 校验可变参数是否只有一个
        int variableArgsNum = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if(parameterType.isArray() || Map.class.isAssignableFrom(parameterType) || Collection.class.isAssignableFrom(parameterType)){
                variableArgsNum +=1 ;
                if(variableArgsNum > 1){
                    if (log.isDebugEnabled()) {
                        log.debug("check command method result : false . reason : {} , methodName : {} , parameterTypes : {}","可变参数不止一个",method.getName(),parameterTypes);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取所有公共方法
     * @param clazz clazz
     * @return 所有公共静态方法
     */
    public static List<Method> getAllPublicMethod(Class<?> clazz){
        List<Method> list = new ArrayList<>();
        // 获取所有公共方法
        Method[] methods = clazz.getMethods();
        // 遍历方法并筛选出静态方法
        for (Method method : methods) {
            list.add(method);
        }
        return list;
    }

}
