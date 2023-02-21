package com.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication(scanBasePackages={"com"})
public class FinanceManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceManagerApplication.class, args);
    }
}
