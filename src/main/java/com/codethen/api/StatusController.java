package com.codethen.api;

import jakarta.ws.rs.Produces;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    /** Simple message, just to know that the new version is up */
    @GetMapping("status")
    @Produces("plain/text")
    public String status() {
        return "v0.7.0 - Translation via Google";
    }
}
