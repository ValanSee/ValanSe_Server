package com.valanse.valanse.common.message;

public enum StorageErrorMessage {
    IMAGE_FILE_READ_FAILED("이미지 파일을 읽을 수 없습니다."),
    IMAGE_UPLOAD_FAILED("이미지 업로드에 실패했습니다."),
    IMAGE_FILE_REQUIRED("업로드할 이미지 파일을 선택해주세요."),
    IMAGE_SIZE_EXCEEDED("이미지는 5MB 이하만 업로드할 수 있습니다."),
    IMAGE_CONTENT_TYPE_INVALID("jpg, png, webp, gif 이미지 파일만 업로드할 수 있습니다.");

    private final String message;

    StorageErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
