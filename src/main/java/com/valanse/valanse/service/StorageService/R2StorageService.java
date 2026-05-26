package com.valanse.valanse.service.StorageService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
            throw new ApiException("이미지 파일을 읽을 수 없습니다.", HttpStatus.BAD_REQUEST);
        } catch (S3Exception e) {
            throw new ApiException("이미지 업로드에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        return properties.getPublicUrl().replaceAll("/+$", "") + "/" + objectKey;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("업로드할 이미지 파일을 선택해주세요.", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ApiException("이미지는 5MB 이하만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new ApiException("jpg, png, webp, gif 이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
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
}
