package com.codethen.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StatusController {

    /** Simple message, just to know that the new version is up */
    @GetMapping("status")
    fun status() : String {
        return "v0.4.0 - Display flags"
    }
}