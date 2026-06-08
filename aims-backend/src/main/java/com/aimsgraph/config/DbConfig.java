package com.aimsgraph.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.aimsgraph.domain.**.mapper")
public class DbConfig {
}
