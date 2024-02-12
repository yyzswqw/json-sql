package json.sql.entity;

import com.jayway.jsonpath.DocumentContext;
import json.sql.config.TableConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class TableContext implements Serializable {

    public static final Map<String,Object> defaultConfig = new HashMap<>();

    /**
     * 原始的json字符串
     */
    private String originalJson;

    /**
     * TableConfig.WRITE_MODEL 为 false 时，操作的是当前这个DocumentContext
     */
    private DocumentContext document;

    /**
     * copy on write 的 DocumentContext，TableConfig.WRITE_MODEL 为 true 时，有值，并且操作的是当前这个DocumentContext
     */
    private DocumentContext newDocument;

    private Map<String,Object> config = new HashMap<>();

    public void setConfig(String key,Object value){
        this.config.put(key,value);
    }

    static {
        defaultConfig.put(TableConfig.WRITE_MODEL, false);
    }




}
