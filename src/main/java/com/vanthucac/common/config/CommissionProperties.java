package com.vanthucac.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.commission")
public record CommissionProperties(BigDecimal rate) {
}