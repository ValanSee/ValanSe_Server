package com.valanse.valanse.service.StorageService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.config.R2Properties;
import com.valanse.valanse.common.message.StorageErrorMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class R2StorageServiceTest {

    private S3Client s3Client;
    private R2StorageService storageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);

        R2Properties properties = new R2Properties();
        properties.setBucket("test-bucket");
        properties.setPublicUrl("https://cdn.example.com/");

        storageService = new R2StorageService(s3Client, properties);
    }

    @Test
    void 이미지를_R2에_업로드하고_공개_URL을_반환한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "image".getBytes()
        );

        String imageUrl = storageService.uploadImage(file, "images");

        assertThat(imageUrl).startsWith("https://cdn.example.com/images/");
        assertThat(imageUrl).endsWith(".png");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void 이미지가_아닌_파일은_업로드하지_않는다() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "text".getBytes()
        );

        ApiException exception = assertThrows(ApiException.class, () -> storageService.uploadImage(file, "images"));
        assertThat(exception.getMessage()).isEqualTo(StorageErrorMessage.IMAGE_CONTENT_TYPE_INVALID.message());
    }

    @Test
    void R2_공개_URL이면_object_key를_추출해_이미지를_삭제한다() {
        storageService.deleteImageByUrl("https://cdn.example.com/member_profile_image/old.png");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void R2_공개_URL이_아니면_이미지를_삭제하지_않는다() {
        storageService.deleteImageByUrl("https://k.kakaocdn.net/profile/image.png");

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }
}
