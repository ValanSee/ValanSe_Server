package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.dto.Title.TitleCreateRequest;
import com.valanse.valanse.dto.Title.TitleCreateResponse;
import com.valanse.valanse.dto.Title.TitleAdminResponse;
import com.valanse.valanse.dto.Title.TitleDeleteResponse;
import com.valanse.valanse.dto.Title.TitleEquipResponse;
import com.valanse.valanse.dto.Title.TitleListResponse;
import com.valanse.valanse.dto.Title.TitlePurchaseResponse;
import com.valanse.valanse.dto.Title.TitleUpdateRequest;
import com.valanse.valanse.dto.Title.TitleUpdateResponse;

import java.util.List;

/**
 * TitleService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface TitleService {
    TitleListResponse getTitleList(Long userId);

    TitleEquipResponse equipTitle(Long userId, Long titleId);

    TitlePurchaseResponse purchaseTitle(Long userId, Long titleId);

    List<TitleAdminResponse> getTitleListForAdmin(Long adminUserId);

    TitleCreateResponse createTitle(Long adminUserId, TitleCreateRequest request);

    TitleUpdateResponse updateTitle(Long adminUserId, Long titleId, TitleUpdateRequest request);

    TitleDeleteResponse deleteTitle(Long adminUserId, Long titleId);
}
