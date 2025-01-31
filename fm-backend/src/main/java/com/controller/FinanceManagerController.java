package com.controller;

import com.service.FinanceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Dictionary;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/home")
public class FinanceManagerController {

    @Autowired
    private FinanceManagerService financeManagerService;

    @GetMapping
    public ResponseEntity<String> statusCheck() {
        return new ResponseEntity<>("Finance Manager is running", HttpStatus.OK);
    }

    @GetMapping("/account/overview")
    public ResponseEntity<Dictionary<String, BigDecimal>> getAccountOverview() {
        return new ResponseEntity<>(financeManagerService.getAccountOverview(), HttpStatus.OK);
    }
}
