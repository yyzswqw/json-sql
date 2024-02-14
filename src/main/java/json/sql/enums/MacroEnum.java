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
    /*
     * 所有的注册的udf函数描述信息
     * java.util.Collection<UdfFunctionDescInfo> 类型
     */
    ,ALL_UDF_DESC_INFO
    /*
     * 所有注册的表名
     * java.util.Set<String> 类型
     */
    ,ALL_TABLE_NAME
    /*
     * 获取指定表不可操作的数据，不可操作意味着对该对象的修改不会影响现有的表中的数据，需搭配宏参数CUR_CONTEXT_PROXY使用
     * java.util.Map<String,com.jayway.jsonpath.DocumentContext> 类型
     */
    ,TABLE_NOT_OPERABLE_DATA
    /*
     * 获取指定表可操作的数据，可操作意味着对该对象的修改会影响现有的表中的数据
     * java.util.Map<String,com.jayway.jsonpath.DocumentContext> 类型，需搭配宏参数CUR_CONTEXT_PROXY使用
     */
    ,TABLE_OPERABLE_DATA
    /*
     * 当前上下文代理
     * json.sql.CurContextProxy 类型
     */
    ,CUR_CONTEXT_PROXY



}
