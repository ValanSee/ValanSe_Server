package com.valanse.valanse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.api.GlobalExceptionHandler;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;
import com.valanse.valanse.domain.enums.MbtiIe;
import com.valanse.valanse.domain.enums.MbtiTf;
import com.valanse.valanse.dto.MemberProfile.MemberProfileImageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberBlock.MemberBlockListResponse;
import com.valanse.valanse.service.MemberBlockService.MemberBlockService;
import com.valanse.valanse.service.MemberProfileService.MemberProfileService;
import com.valanse.valanse.service.PointService.PointService;
import com.valanse.valanse.service.TitleService.TitleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MemberProfileService memberProfileService;

    @Mock
    private PointService pointService;

    @Mock
    private TitleService titleService;

    @Mock
    private MemberBlockService memberBlockService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long memberId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(memberId), null, List.of()));
    }

    @Test
    @DisplayName("회원 차단 시 로그인 회원 id로 차단하고 blocked=true를 반환한다")
    void blockMember_ReturnsBlockedTrue() throws Exception {
        loginAs(1L);

        mockMvc.perform(post("/member/blocks/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        verify(memberBlockService).block(1L, 2L);
    }

    @Test
    @DisplayName("자기 자신을 차단하면 400을 반환한다")
    void blockMember_Self_ReturnsBadRequest() throws Exception {
        loginAs(1L);
        doThrow(new ApiException(MemberErrorMessage.CANNOT_BLOCK_SELF.message(), HttpStatus.BAD_REQUEST))
                .when(memberBlockService).block(1L, 1L);

        mockMvc.perform(post("/member/blocks/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(MemberErrorMessage.CANNOT_BLOCK_SELF.message()));
    }

    @Test
    @DisplayName("회원 차단 해제 시 blocked=false를 반환한다")
    void unblockMember_ReturnsBlockedFalse() throws Exception {
        loginAs(1L);

        mockMvc.perform(delete("/member/blocks/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));

        verify(memberBlockService).unblock(1L, 2L);
    }

    @Test
    @DisplayName("차단 목록 조회는 blocks 배열로 memberId/nickname/blockedAt을 반환한다")
    void getBlocks_ReturnsBlockList() throws Exception {
        loginAs(1L);
        when(memberBlockService.getBlocks(1L)).thenReturn(new MemberBlockListResponse(List.of(
                new MemberBlockListResponse.BlockedMember(3L, "차단된닉", LocalDateTime.of(2026, 11, 16, 12, 0)),
                new MemberBlockListResponse.BlockedMember(2L, null, LocalDateTime.of(2026, 11, 15, 12, 0))
        )));

        mockMvc.perform(get("/member/blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks.length()").value(2))
                .andExpect(jsonPath("$.blocks[0].memberId").value(3))
                .andExpect(jsonPath("$.blocks[0].nickname").value("차단된닉"))
                .andExpect(jsonPath("$.blocks[0].blockedAt").exists())
                .andExpect(jsonPath("$.blocks[1].nickname").value(org.hamcrest.Matchers.nullValue()));
    }

    @BeforeEach
    void setUp() {
        MemberController memberController = new MemberController(
                memberProfileService,
                pointService,
                titleService,
                memberBlockService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new GlobalExceptionHandler(event -> { }))
                .build();
    }

    @Test
    @DisplayName("프로필 저장 시 닉네임이 비어있으면 공통 메시지로 400 응답을 반환한다")
    void saveProfile_EmptyNickname_ReturnsBadRequestWithMemberErrorMessage() throws Exception {
        MemberProfileRequest request = new MemberProfileRequest(
                " ",
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENTP"
        );

        mockMvc.perform(post("/member/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(MemberErrorMessage.NICKNAME_REQUIRED.message()))
                .andExpect(jsonPath("$.status").value(400));

        verify(memberProfileService, never()).saveOrUpdateProfile(request);
    }

    @Test
    @DisplayName("프로필 저장 시 MBTI가 불완전하면 공통 메시지로 400 응답을 반환한다")
    void saveProfile_InvalidMbti_ReturnsBadRequestWithMemberErrorMessage() throws Exception {
        MemberProfileRequest request = new MemberProfileRequest(
                "테스터",
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENT"
        );

        mockMvc.perform(post("/member/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(MemberErrorMessage.MBTI_INVALID_LENGTH.message()))
                .andExpect(jsonPath("$.status").value(400));

        verify(memberProfileService, never()).saveOrUpdateProfile(request);
    }

    @Test
    @DisplayName("프로필 이미지 수정 시 multipart 파일을 받아 이미지 URL을 반환한다")
    void updateProfileImage_ReturnsProfileImageUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "image".getBytes()
        );
        MemberProfileImageResponse response = new MemberProfileImageResponse(
                "https://cdn.example.com/member_profile_image/profile.png"
        );

        when(memberProfileService.updateProfileImage(file)).thenReturn(response);

        mockMvc.perform(multipart("/member/profile-image")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile_image_url").value("https://cdn.example.com/member_profile_image/profile.png"));

        verify(memberProfileService).updateProfileImage(file);
    }
}
