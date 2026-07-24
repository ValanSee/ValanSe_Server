package com.valanse.valanse.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@Profile({"local", "dev", "dev-server"})
@RequestMapping("/admin/test")
public class ServerErrorTestController {

    @GetMapping("/server-error")
    public void serverError() {
        throw new IllegalStateException("Discord server error alert test");
    }
}
