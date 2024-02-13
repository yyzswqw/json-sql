package json.sql.udf;


import java.util.Map;
import java.util.Objects;

/**
 * Map类型泛型定义
 */
public class MapTypeReference extends BaseTypeReference {

    private Class<?> keyType = Object.class;

    private Class<?> valueType = Object.class;

    public MapTypeReference(Class<?> keyType,Class<?> valueType) {
        if(Objects.nonNull(keyType)){
            this.keyType = keyType;
        }
        if(Objects.nonNull(valueType)){
            this.valueType = valueType;
        }
    }

    @Override
    public Class<?> getArgType() {
        return Map.class;
    }

    @Override
    public Class<?> getArgGenericityType1() {
        return keyType;
    }

    @Override
    public Class<?> getArgGenericityType2() {
        return valueType;
    }
}
