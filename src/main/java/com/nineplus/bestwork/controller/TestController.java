package com.nineplus.bestwork.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class TestController {

    @GetMapping("/test")
    public ResponseEntity<?> testController() {
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
