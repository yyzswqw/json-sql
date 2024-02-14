package json.sql.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class UdfParamDescInfo implements Serializable {

    private String paramName;

    private String paramType;

    private String paramDesc;

}
