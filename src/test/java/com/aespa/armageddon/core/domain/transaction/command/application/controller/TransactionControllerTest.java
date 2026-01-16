package com.aespa.armageddon.core.domain.transaction.command.application.controller;

import com.aespa.armageddon.core.domain.transaction.command.application.dto.request.TransactionEditRequest;
import com.aespa.armageddon.core.domain.transaction.command.application.dto.request.TransactionWriteRequest;
import com.aespa.armageddon.core.domain.transaction.command.application.service.TransactionService;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false) // Security Filter 비활성화 (순수 컨트롤러 로직 검증)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TOKEN = "Bearer test-token";
    private static final Long USER_NO = 1L;

    @Test
    @DisplayName("거래 생성 성공")
    void writeTransaction_Success() throws Exception {
        // given
        TransactionWriteRequest request = new TransactionWriteRequest(
                "점심", "메모", 5000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(USER_NO);

        // when & then
        mockMvc.perform(post("/transaction/write")
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(transactionService).writeTransaction(eq(USER_NO), any(TransactionWriteRequest.class));
    }

    @Test
    @DisplayName("거래 수정 성공")
    void editTransaction_Success() throws Exception {
        // given
        Long transactionId = 100L;
        TransactionEditRequest request = new TransactionEditRequest(
                "저녁", "메모", 12000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(USER_NO);

        // when & then
        mockMvc.perform(put("/transaction/edit/{transactionId}", transactionId)
                .header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(transactionService).editTransaction(eq(USER_NO), eq(transactionId), any(TransactionEditRequest.class));
    }

    @Test
    @DisplayName("거래 삭제 성공")
    void deleteTransaction_Success() throws Exception {
        // given
        Long transactionId = 100L;
        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(USER_NO);

        // when & then
        mockMvc.perform(delete("/transaction/delete/{transactionId}", transactionId)
                .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(transactionService).deleteTransaction(USER_NO, transactionId);
    }
}
