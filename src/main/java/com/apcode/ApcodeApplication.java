package com.apcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ApcodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApcodeApplication.class, args);
        System.out.println("""
                
                ╔══════════════════════════════════════════╗
                ║     AP_Code Backend Started! ✅           ║
                ║     API Base: http://localhost:8080/api   ║
                ║     Docs:     /api/swagger-ui.html        ║
                ╚══════════════════════════════════════════╝
                """);
    }
}
