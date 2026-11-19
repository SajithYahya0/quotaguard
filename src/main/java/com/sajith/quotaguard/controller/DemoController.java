package com.sajith.quotaguard.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
public class DemoController {

    @GetMapping("/quote")
    public Map<String, String> quote() {
        return Map.of("quote", "gng.", "servedAt", Instant.now().toString());
    }
}
