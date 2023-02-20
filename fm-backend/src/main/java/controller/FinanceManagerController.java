package controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import service.FinanceManagerService;


@RestController
public class FinanceManagerController {
    @GetMapping("/")
    public String firstTry() {
        System.out.print("Hi");
        return "Welcome Friend";
    }

    @GetMapping("/no")
    public ResponseEntity<String> getHome() {
        System.out.print("Hi");
        return new ResponseEntity<>("<h1>The Finance Manager is running</h1>", HttpStatus.OK);
    }


    @GetMapping("/salary")
    public ResponseEntity<Integer> getCurrentSalary() {
        System.out.print("Salary!!");
        FinanceManagerService service = new FinanceManagerService();
        return new ResponseEntity<>(service.getCurrentSalary(), HttpStatus.OK);
    }
}
