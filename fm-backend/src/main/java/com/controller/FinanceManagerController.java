package com.controller;

import com.service.FinanceManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/home")
public class FinanceManagerController {

    private final FinanceManagerService financeManagerService;

    public FinanceManagerController(FinanceManagerService financeManagerService) {
        this.financeManagerService = financeManagerService;
    }

    @GetMapping("/")
    public ResponseEntity<String> status_check() {
        return new ResponseEntity<>("Finance Manager is running", HttpStatus.OK);
    }

    @GetMapping("/salary")
    public ResponseEntity<Integer> current_salary() {
        return new ResponseEntity<>(financeManagerService.getCurrentSalary(), HttpStatus.OK);
    }
}
