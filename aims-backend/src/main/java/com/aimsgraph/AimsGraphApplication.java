package com.aimsgraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;

import org.springframework.scheduling.annotation.EnableScheduling;

import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableScheduling
public class AimsGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(AimsGraphApplication.class, args);
    }

    /**
     * Java 21 Virtual Threads 활성화 (Tomcat)
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
