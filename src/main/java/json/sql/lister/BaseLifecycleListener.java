package json.sql.lister;

import json.sql.JsonSqlContext;
import json.sql.entity.UdfFunctionDescInfo;

import java.lang.reflect.Method;

public class BaseLifecycleListener implements LifecycleListener {

    @Override
    public void registerOneUdfFinish(Method method, UdfFunctionDescInfo udfDescInfo) {

    }

    @Override
    public void innerRegisterUdfFinish(JsonSqlContext jsonSqlContext) {

    }
}
