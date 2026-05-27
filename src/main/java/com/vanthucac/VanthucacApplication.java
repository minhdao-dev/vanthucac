package com.vanthucac;

import com.vanthucac.common.config.AwsProperties;
import com.vanthucac.common.config.CommissionProperties;
import com.vanthucac.common.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, AwsProperties.class, CommissionProperties.class})
public class VanthucacApplication {

    public static void main(String[] args) {
        SpringApplication.run(VanthucacApplication.class, args);
    }
}