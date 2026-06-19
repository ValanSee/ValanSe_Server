package com.valanse.valanse.service.StorageService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.config.R2Properties;
import com.valanse.valanse.common.message.StorageErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Cloudflare R2에 이미지 파일을 업로드하고 공개 URL을 생성하는 스토리지 서비스 코드입니다.
 * check: MIME 타입뿐 아니라 실제 이미지 시그니처 검증을 추가하는 것이 안전합니다.
 */
public class R2StorageService implements StorageService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final S3Client s3Client;
    private final R2Properties properties;

    /**
     * 이미지 파일을 검증한 뒤 Cloudflare R2에 업로드하고 공개 URL을 반환하는 메서드입니다.
     * check: 파일 내용 기반 이미지 검증과 악성 파일 차단 정책을 추가해야 합니다.
     */
    @Override
    public String uploadImage(MultipartFile file, String directory) {
        validateImage(file);

        String objectKey = buildObjectKey(file, directory);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new ApiException(StorageErrorMessage.IMAGE_FILE_READ_FAILED.message(), HttpStatus.BAD_REQUEST);
        } catch (S3Exception e) {
            log.warn(
                    "R2 image upload failed. bucket={}, key={}, statusCode={}, errorCode={}, requestId={}, message={}",
                    properties.getBucket(),
                    objectKey,
                    e.statusCode(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null,
                    e.requestId(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage()
            );
            throw new ApiException(StorageErrorMessage.IMAGE_UPLOAD_FAILED.message(), HttpStatus.BAD_GATEWAY);
        }

        return properties.getPublicUrl().replaceAll("/+$", "") + "/" + objectKey;
    }

    @Override
    public void deleteImageByUrl(String imageUrl) {
        String objectKey = extractObjectKey(imageUrl);
        if (!StringUtils.hasText(objectKey)) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            log.warn(
                    "R2 image delete failed. bucket={}, key={}, statusCode={}, errorCode={}, requestId={}, message={}",
                    properties.getBucket(),
                    objectKey,
                    e.statusCode(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null,
                    e.requestId(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage()
            );
            throw new ApiException(StorageErrorMessage.IMAGE_DELETE_FAILED.message(), HttpStatus.BAD_GATEWAY);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(StorageErrorMessage.IMAGE_FILE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ApiException(StorageErrorMessage.IMAGE_SIZE_EXCEEDED.message(), HttpStatus.BAD_REQUEST);
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new ApiException(StorageErrorMessage.IMAGE_CONTENT_TYPE_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private String buildObjectKey(MultipartFile file, String directory) {
        String cleanDirectory = StringUtils.hasText(directory)
                ? directory.replaceAll("^/+|/+$", "")
                : "images";
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String filename = extension == null
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + extension.toLowerCase();

        return cleanDirectory + "/" + filename;
    }

    private String extractObjectKey(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }

        String publicUrl = properties.getPublicUrl().replaceAll("/+$", "");
        String prefix = publicUrl + "/";
        if (!imageUrl.startsWith(prefix)) {
            return null;
        }

        String objectKey = imageUrl.substring(prefix.length());
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }

        return URLDecoder.decode(objectKey, StandardCharsets.UTF_8);
    }
}
