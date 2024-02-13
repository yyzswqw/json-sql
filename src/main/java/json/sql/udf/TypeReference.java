package json.sql.udf;

/**
 * udf可变参数泛型定义
 */
public interface TypeReference {

    /**
     * 获取参数外层类型
     * @return class
     */
    public Class<?> getArgType();

    /**
     * 获取参数第一个泛型类型，Collection、Map中key的泛型
     * @return class
     */
    public Class<?> getArgGenericityType1();

    /**
     * 获取参数第二个泛型类型，Map中value的泛型
     * @return class
     */
    public Class<?> getArgGenericityType2();
}
