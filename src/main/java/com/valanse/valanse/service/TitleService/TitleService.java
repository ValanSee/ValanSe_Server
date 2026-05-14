package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.dto.Title.TitleCreateRequest;
import com.valanse.valanse.dto.Title.TitleCreateResponse;
import com.valanse.valanse.dto.Title.TitleEquipResponse;
import com.valanse.valanse.dto.Title.TitleListResponse;
import com.valanse.valanse.dto.Title.TitlePurchaseResponse;

public interface TitleService {
    TitleListResponse getTitleList(Long userId);

    TitleEquipResponse equipTitle(Long userId, Long titleId);

    TitlePurchaseResponse purchaseTitle(Long userId, Long titleId);

    TitleCreateResponse createTitle(Long adminUserId, TitleCreateRequest request);
}
