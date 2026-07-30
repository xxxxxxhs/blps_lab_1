package ru.blps.lab_1.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/util")
public class UtilController {

    public static final Logger logger = LoggerFactory.getLogger(UtilController.class);

    @Value("${spring.application.instance-num}")
    private String instanceNum;
    
    private Integer attempt = 1;

    @GetMapping("/info")
    public ResponseEntity<String> info() {
        logger.atInfo().addKeyValue("attempt", this.attempt++).log("==SIMPLE API CALL==");
        return ResponseEntity.ok("Instancenumber: " + instanceNum);
    }
}
