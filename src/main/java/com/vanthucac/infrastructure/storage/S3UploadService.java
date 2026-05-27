package com.vanthucac.infrastructure.storage;

import com.vanthucac.common.config.AwsProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class S3UploadService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    public S3UploadService(S3Presigner s3Presigner, AwsProperties awsProperties) {
        this.s3Presigner = s3Presigner;
        this.awsProperties = awsProperties;
    }

    public PresignedUrlResponse generatePresignedUrl(
            String originalFileName,
            String contentType,
            Long userId
    ) {
        validateContentType(contentType);

        var sanitizedFileName = sanitizeFileName(originalFileName);
        var fileKey = "listings/" + userId + "/" + UUID.randomUUID() + "/" + sanitizedFileName;
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

    private void validateContentType(String contentType) {
        if (!PresignedUrlRequest.ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Content type not allowed: " + contentType
                            + ". Allowed: " + PresignedUrlRequest.ALLOWED_CONTENT_TYPES
            );
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload_" + UUID.randomUUID();
        }
        return UNSAFE_FILENAME_CHARS.matcher(fileName).replaceAll("_");
    }

    public record PresignedUrlResponse(
            String uploadUrl,
            String fileKey,
            String fileUrl
    ) {
    }
}