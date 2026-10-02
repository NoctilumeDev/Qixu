package dev.noctilume.qixu.preparation;

import java.math.BigInteger;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Domain normal form: sorted object keys, ordered arrays, integer-only numbers, UTF-8, no newline. */
public final class FrozenJson {
    private final JsonMapper json;
    public FrozenJson(JsonMapper json) {this.json=json;}
    public String encode(Object value) {return json.writeValueAsString(sorted(json.readValue(json.writeValueAsString(value),Object.class)));}
    public String normalize(String value) {return json.writeValueAsString(sorted(json.readValue(value,Object.class)));}
    private Object sorted(Object value) {
        if(value instanceof Map<?,?> map) {var result=new TreeMap<String,Object>();map.forEach((key,child)->result.put(key.toString(),sorted(child)));return result;}
        if(value instanceof List<?> list)return list.stream().map(this::sorted).toList();
        if(value==null || value instanceof String || value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof BigInteger)return value;
        throw new IllegalArgumentException("Frozen JSON accepts only domain integer scalars");
    }
}
