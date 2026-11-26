package com.taskflow.backend.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController                 // every method returns data (JSON), not an HTML view
@RequestMapping("/api/public")  // URL prefix: "public" = no login needed (matters in Phase 2)
public class PingController {

    @GetMapping("/ping")        // handles GET /api/public/ping
    public Map<String, String> ping() {
        // Jackson (bundled with Spring Web) turns this Map into {"status":"ok",...}
        return Map.of("status", "ok", "service", "taskflow-backend");
    }
}
