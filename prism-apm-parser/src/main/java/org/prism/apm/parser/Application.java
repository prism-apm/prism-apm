package org.prism.apm.parser;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author peter
 * @create 2019-07-01
 */

@EnableScheduling
@MapperScan({"org.prism.apm.parser.repository", "org.prism.apm.common.repository"})
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
