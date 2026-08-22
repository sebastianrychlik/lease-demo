package com.leasedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.leasedemo.exchange.mapper.ExchangeRateMapper;
import com.leasedemo.exchange.mapper.ExchangeRateMapperImpl;

@SpringBootApplication
public class LeaseDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaseDemoApplication.class, args);
    }

}
