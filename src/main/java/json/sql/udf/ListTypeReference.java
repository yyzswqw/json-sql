package json.sql.udf;


import java.util.Collection;
import java.util.Objects;

/**
 * Collection类型泛型定义
 */
public class ListTypeReference extends BaseTypeReference {

    private Class<?> type = Object.class;

    public ListTypeReference(Class<?> type) {
        if(Objects.nonNull(type)){
            this.type = type;
        }
    }

    @Override
    public Class<?> getArgType() {
        return Collection.class;
    }

    @Override
    public Class<?> getArgGenericityType1() {
        return type;
    }

}
