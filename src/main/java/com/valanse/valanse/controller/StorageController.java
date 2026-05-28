package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Storage.ImageUploadResponse;
import com.valanse.valanse.service.StorageService.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "파일 업로드 API", description = "이미지 업로드 관련 기능")
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @Operation(
            summary = "이미지 업로드",
            description = "이미지 파일을 Cloudflare R2에 업로드하고 공개 URL을 반환합니다."
    )
    @PostMapping(value = "/images", consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        String imageUrl = storageService.uploadImage(file, "images");
        return ResponseEntity.ok(new ImageUploadResponse(imageUrl));
    }
}
