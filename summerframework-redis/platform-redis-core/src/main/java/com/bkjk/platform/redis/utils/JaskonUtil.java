package com.bkjk.platform.redis.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JaskonUtil {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T deSerialize(String json, Class<T> clazz) throws IOException {
        return MAPPER.readValue(json, clazz);
    }

    public static String serialize(Object o) throws JsonProcessingException {
        String json = MAPPER.writeValueAsString(o);
        return json;
    }
}
