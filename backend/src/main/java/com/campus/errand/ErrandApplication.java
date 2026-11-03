package com.campus.errand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园跑腿任务发布与接单系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.campus.errand.mapper")
public class ErrandApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErrandApplication.class, args);
    }
}
