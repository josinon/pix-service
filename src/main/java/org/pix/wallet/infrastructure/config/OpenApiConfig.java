package org.pix.wallet.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI walletApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Pix Wallet API")
                .description("Documentação da API Pix Wallet")
                .version("v1"));
    }
}
