package com.sajith.quotagaurd.controller;

import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DemoController {

    @GetMapping("/quote")
    public Map<String, String> quote() {
        return Map.of(
                "quote", "gng.",
                "servedAt", Instant.now().toString()
        );
    }
}