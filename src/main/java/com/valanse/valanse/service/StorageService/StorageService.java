package com.valanse.valanse.service.StorageService;

import org.springframework.web.multipart.MultipartFile;

/**
 * StorageService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface StorageService {
    String uploadImage(MultipartFile file, String directory);
    void deleteImageByUrl(String imageUrl);
}
