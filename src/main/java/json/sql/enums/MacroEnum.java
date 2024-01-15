package json.sql.enums;

/**
 * udf 宏定义
 */
public enum MacroEnum {

    /*
     * 原始的json
     * java.long.String 类型
     */
    ORIGINAL_JSON
    /*
     * 读取值的json document
     * com.jayway.jsonpath.DocumentContext 类型
     */
    ,READ_DOCUMENT
    /*
      * 原始的写操作的json document
      * com.jayway.jsonpath.DocumentContext 类型
     */
    ,ORIGINAL_WRITE_DOCUMENT
    /*
     * copy写方式的写操作的json document
     * com.jayway.jsonpath.DocumentContext 类型
     */
    ,COPY_WRITE_WRITE_DOCUMENT
    /*
     * 当前写操作的json document，根据写模式的不同，获取到的是当前写，操作的那个json document
     * com.jayway.jsonpath.DocumentContext 类型
     */
    ,CUR_WRITE_DOCUMENT



}
