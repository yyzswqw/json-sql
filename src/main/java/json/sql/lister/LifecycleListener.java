package json.sql.lister;

import json.sql.JsonSqlContext;
import json.sql.entity.UdfFunctionDescInfo;

import java.lang.reflect.Method;

public interface LifecycleListener {

    /**
     * 一个udf 函数注册完成时回调
     * @param method udf函数具体的方法
     * @param udfDescInfo udf函数的描述信息
     */
    void registerOneUdfFinish(Method method, UdfFunctionDescInfo udfDescInfo);


    /**
     * 内部Udf 函数注册完成时回调
     * @param jsonSqlContext 上下文
     */
    void innerRegisterUdfFinish(JsonSqlContext jsonSqlContext);



}
