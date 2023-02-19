package controller;

import service.FinanceManagerService;

public class FinanceManagerController {
    FinanceManagerService service;

    public FinanceManagerController() {
        this.service = new FinanceManagerService();
    }

    public int run() {
        System.out.print("Hi");
        return service.getCurrentSalary();
    }
}
