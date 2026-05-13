package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.dto.Title.TitleEquipResponse;
import com.valanse.valanse.dto.Title.TitleListResponse;

public interface TitleService {
    TitleListResponse getTitleList(Long userId);

    TitleEquipResponse equipTitle(Long userId, Long titleId);
}
