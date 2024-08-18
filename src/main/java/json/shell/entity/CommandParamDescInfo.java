package json.shell.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommandParamDescInfo implements Serializable {

    private String paramName;

    private String paramType;

    private String paramDesc;

    /**
     * 参数类型
     */
    private Class<?> argsType;

    /**
     * 泛型类型列表
     */
    private List<Class<?>> genericityTypeList = new ArrayList<>();



}
