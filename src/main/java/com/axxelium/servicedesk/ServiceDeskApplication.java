package com.axxelium.servicedesk;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "ServiceDesk Pilot API", version = "1.0",
        description = "Учебный API техподдержки: заявки, инженеры и комментарии. Без авторизации."))
public class ServiceDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceDeskApplication.class, args);
    }
}
