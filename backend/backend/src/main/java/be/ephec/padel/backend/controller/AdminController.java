package be.ephec.padel.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "status", "ok",
                "message", "Admin endpoint sécurisé"
        );
    }
}