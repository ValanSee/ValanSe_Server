package com.valanse.valanse.dto.Title;

/**
 * TitleEquipResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record TitleEquipResponse(
        Long titleId,
        String title,
        boolean equipped
) {
}
