package com.valanse.valanse.dto.Vote;

import com.valanse.valanse.domain.enums.PinType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PinRequest {
    private PinType pinType;

}
