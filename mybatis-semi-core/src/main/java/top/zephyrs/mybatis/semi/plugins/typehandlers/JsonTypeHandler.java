package top.zephyrs.mybatis.semi.plugins.typehandlers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonTypeHandler extends AbstractJsonTypeHandler {
    private static ObjectMapper mapper;

    public JsonTypeHandler(Class<?> type) {
        super(type);
    }


    public static ObjectMapper getObjectMapper() {
        if (null == mapper) {
            mapper = new ObjectMapper();
        }
        return mapper;
    }

    @Override
    public String toJson(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("serialize " + obj + " to json error ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object fromJson(String json, Class<?> type) {
        ObjectMapper objectMapper = getObjectMapper();
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException e) {
            log.error("deserialize json: " + json + " to " + type + " error ", e);
            throw new RuntimeException(e);
        }
    }
}
