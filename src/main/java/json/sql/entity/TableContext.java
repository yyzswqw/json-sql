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

    private String originalJson;

    private DocumentContext document;

    private DocumentContext newDocument;

    private Map<String,Object> config = new HashMap<>();

    public void setConfig(String key,Object value){
        this.config.put(key,value);
    }

    static {
        defaultConfig.put(TableConfig.WRITE_MODEL, false);
    }




}
