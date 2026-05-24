package com.vanthucac.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(
        String accessKey,
        String secretKey,
        String region,
        S3Properties s3
) {
    public record S3Properties(String bucket) {
    }
}