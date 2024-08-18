package json.shell.entity;


import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommandDescInfo implements Serializable {

    private String name;

    private Object instance;

    private Method method;

    private String functionDesc;

    private String returnType;

    private final List<CommandParamDescInfo> commandParamDescInfoList = new ArrayList<>();

}
