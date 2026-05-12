package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.dto.Title.TitleEquipResponse;

public interface TitleService {
    TitleEquipResponse equipTitle(Long userId, Long titleId);
}
