package com.cityguard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cityguard.mapper")
public class CityguardApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityguardApplication.class, args);
    }
}