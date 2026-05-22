package com.valanse.valanse.service.StorageService;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadImage(MultipartFile file, String directory);
}
