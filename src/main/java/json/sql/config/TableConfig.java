package json.sql.config;

import java.io.Serializable;

public class TableConfig implements Serializable {

//    设置值和读取值的模式，false：直接操作原始数据，true：读在原始数据上，写在新数据上，set时，即使值被改了，读取到的还是原始的值
    public static final String WRITE_MODEL = "writeModel";
}
