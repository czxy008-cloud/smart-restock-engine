package com.smartrestock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartrestock.mapper")
public class SmartRestockApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartRestockApplication.class, args);
    }
}
