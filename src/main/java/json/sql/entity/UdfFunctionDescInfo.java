package json.sql.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class UdfFunctionDescInfo implements Serializable {

    private String functionName;

    private String sourceByClass;

    private String functionDesc;

    private String returnType;

    private List<UdfParamDescInfo> udfParamDescInfoList = new ArrayList<>();

}
