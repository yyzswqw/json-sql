package json.sql.udf;


/**
 * 可变参数泛型定义
 */
public class BaseTypeReference implements TypeReference {

    @Override
    public Class<?> getArgType() {
        return Object.class;
    }

    @Override
    public Class<?> getArgGenericityType1() {
        return Object.class;
    }

    @Override
    public Class<?> getArgGenericityType2() {
        return Object.class;
    }
}
