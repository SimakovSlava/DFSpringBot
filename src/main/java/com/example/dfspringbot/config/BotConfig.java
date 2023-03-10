package com.example.dfspringbot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {

    @Value("${bot.username}")
    private String botName;

    @Value("${bot.token}")
    private String token;

    @Value("${bot.admin}")
    private Long adminId;
}
