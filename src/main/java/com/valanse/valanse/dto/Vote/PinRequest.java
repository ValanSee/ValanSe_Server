package com.valanse.valanse.dto.Vote;

import com.valanse.valanse.domain.enums.PinType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/**
 * PinRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public class PinRequest {
    private PinType pinType;

}
