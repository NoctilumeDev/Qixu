package dev.noctilume.qixu.preparation;

import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class FrozenJsonTest {
    @Test void mysqlJsonKeyReorderingDoesNotChangeDomainDigestBytes() {
        var codec=new FrozenJson(JsonMapper.builder().build());
        assertEquals("{\"a\":1,\"b\":[null,{\"c\":\"中文\",\"z\":2}]}",codec.normalize("{ \"b\": [null, {\"z\":2,\"c\":\"中文\"}], \"a\":1 }"));
        assertEquals(codec.normalize("{\"x\":1,\"y\":2}"),codec.normalize("{\"y\":2,\"x\":1}"));
        assertThrows(IllegalArgumentException.class,()->codec.normalize("{\"rank\":1.5}"));
    }
}
