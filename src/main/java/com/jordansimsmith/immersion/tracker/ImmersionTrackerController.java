package com.jordansimsmith.immersion.tracker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImmersionTrackerController {

    @GetMapping("/")
    public String index() {
        return "test";
    }
}
