package com.codethen.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StatusController {

    @GetMapping("status")
    fun status() : String {
        return "ok"
    }
}