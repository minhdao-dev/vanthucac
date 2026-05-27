package com.vanthucac.infrastructure.storage;

import com.vanthucac.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final S3UploadService s3UploadService;

    public UploadController(S3UploadService s3UploadService) {
        this.s3UploadService = s3UploadService;
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<S3UploadService.PresignedUrlResponse>> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var userId = Long.parseLong(jwt.getSubject());
        var result = s3UploadService.generatePresignedUrl(
                request.fileName(),
                request.contentType(),
                userId
        );
        return ResponseEntity.ok(ApiResponse.ok("Presigned URL generated", result));
    }
}