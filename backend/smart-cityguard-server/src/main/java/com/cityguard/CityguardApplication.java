package com.cityguard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.cityguard.**.mapper")
@EnableScheduling
public class CityguardApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityguardApplication.class, args);
    }
}