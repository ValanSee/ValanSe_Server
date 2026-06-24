package com.valanse.valanse.dto.Vote;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Swagger 문서에서 투표 생성 multipart/form-data 요청 예시를 표시하기 위한 DTO입니다.
 */
@Getter
@Setter
@Schema(description = "이미지를 포함한 투표 생성 요청")
public class VoteCreateMultipartRequest {

    @Schema(description = "투표 생성 요청 JSON", implementation = VoteCreateRequest.class)
    private VoteCreateRequest request;

    @JsonProperty("option-a-image")
    @Schema(description = "options[].imageKey가 option-a-image인 선택지 이미지", type = "string", format = "binary")
    private MultipartFile optionAImage;

    @JsonProperty("option-b-image")
    @Schema(description = "options[].imageKey가 option-b-image인 선택지 이미지", type = "string", format = "binary")
    private MultipartFile optionBImage;

    @JsonProperty("option-c-image")
    @Schema(description = "options[].imageKey가 option-c-image인 선택지 이미지", type = "string", format = "binary")
    private MultipartFile optionCImage;

    @JsonProperty("option-d-image")
    @Schema(description = "options[].imageKey가 option-d-image인 선택지 이미지", type = "string", format = "binary")
    private MultipartFile optionDImage;
}
