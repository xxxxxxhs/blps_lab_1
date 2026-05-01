package ru.blps.lab_1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/util")
public class UtilController {

    @Value("${spring.application.instance-num}")
    private String instanceNum;
    
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("Instancenumber: " + instanceNum);
    }
}
