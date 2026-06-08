package com.example.lab3392.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.lab3392.mapper")
public class MybatisPlusConfig {}

