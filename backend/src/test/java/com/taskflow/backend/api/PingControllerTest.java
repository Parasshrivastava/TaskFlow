package com.taskflow.backend.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PingControllerTest {
    @Test
    void pingReturnsOk() {
        // Plain unit test: no Spring context, so it runs in milliseconds
        var result = new PingController().ping();
        assertEquals("ok", result.get("status"));
    }
}
