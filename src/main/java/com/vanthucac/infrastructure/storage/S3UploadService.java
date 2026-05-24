package com.vanthucac.infrastructure.storage;

import com.vanthucac.common.config.AwsProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.UUID;

@Service
public class S3UploadService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    public S3UploadService(S3Presigner s3Presigner, AwsProperties awsProperties) {
        this.s3Presigner = s3Presigner;
        this.awsProperties = awsProperties;
    }

    public PresignedUrlResponse generatePresignedUrl(String originalFileName, String contentType) {
        var fileKey = "listings/" + UUID.randomUUID() + "/" + originalFileName;
        var bucket = awsProperties.s3().bucket();

        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(contentType)
                .build();

        var presignedRequest = s3Presigner.presignPutObject(builder -> builder
                .signatureDuration(PRESIGNED_URL_TTL)
                .putObjectRequest(putObjectRequest)
        );

        var fileUrl = "https://" + bucket + ".s3."
                + awsProperties.region() + ".amazonaws.com/" + fileKey;

        return new PresignedUrlResponse(
                presignedRequest.url().toString(),
                fileKey,
                fileUrl
        );
    }

    public record PresignedUrlResponse(
            String uploadUrl,
            String fileKey,
            String fileUrl
    ) {}
}