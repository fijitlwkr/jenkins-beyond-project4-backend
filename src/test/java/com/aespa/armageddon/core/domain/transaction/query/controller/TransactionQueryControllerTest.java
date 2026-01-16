package com.aespa.armageddon.core.domain.transaction.query.controller;

import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionDailyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionLatelyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionSummaryResponse;
import com.aespa.armageddon.core.domain.transaction.query.service.TransactionQueryService;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionQueryService transactionQueryService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // 1. SecurityContext에 들어갈 UserDetails Mocking
        org.springframework.security.core.userdetails.UserDetails mockUserDetails = mock(
                org.springframework.security.core.userdetails.UserDetails.class);
        given(mockUserDetails.getUsername()).willReturn("testUser");
        given(mockUserDetails.getAuthorities()).willReturn(Collections.emptyList());

        // 2. Repository가 반환할 User Entity Mocking
        User mockUserEntity = mock(User.class);
        given(mockUserEntity.getId()).willReturn(1L);

        // Controller가 UserDetails의 username으로 Repository 조회 시 Entity 반환하도록 설정
        given(userRepository.findByLoginId("testUser")).willReturn(Optional.of(mockUserEntity));

        // 3. SecurityContext 설정
        Authentication auth = new UsernamePasswordAuthenticationToken(mockUserDetails, null,
                mockUserDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("최근 거래 내역 조회 API")
    void getLatelyTransactions() throws Exception {
        // given
        // Constructor: id, date, title, amount, category, type
        TransactionLatelyResponse response = new TransactionLatelyResponse(100L, LocalDate.now(), "최근거래", 1000,
                Category.FOOD, TransactionType.EXPENSE);
        given(transactionQueryService.getLatelyTransactions(1L)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/transaction/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].title").value("최근거래"));
    }

    @Test
    @DisplayName("일간 상세 내역 조회 API")
    void getDailyTransactions() throws Exception {
        // given
        // Constructor: id, type, title, amount, category
        TransactionDailyResponse response = new TransactionDailyResponse(100L, TransactionType.EXPENSE, "일간거래", 1000,
                Category.FOOD);
        LocalDate date = LocalDate.of(2024, 5, 20);

        given(transactionQueryService.getDailyTransactions(1L, date)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/transaction/daily")
                .param("date", "2024-05-20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].title").value("일간거래"));
    }

    @Test
    @DisplayName("일간 요약 정보 조회 API")
    void getDailySummary() throws Exception {
        // given
        // Constructor: income, expense
        TransactionSummaryResponse response = new TransactionSummaryResponse(1000L, 500L);
        LocalDate date = LocalDate.of(2024, 5, 20);

        given(transactionQueryService.getDailySummary(1L, date)).willReturn(response);

        // when & then
        mockMvc.perform(get("/transaction/daily/summary")
                .param("date", "2024-05-20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.balance").value(500));
    }

    @Test
    @DisplayName("월간 요약 정보 조회 API")
    void getMonthlySummary() throws Exception {
        // given
        TransactionSummaryResponse response = new TransactionSummaryResponse(10000L, 5000L);
        int year = 2024;
        int month = 5;

        given(transactionQueryService.getMonthlySummary(1L, year, month)).willReturn(response);

        // when & then
        mockMvc.perform(get("/transaction/monthly")
                .param("year", String.valueOf(year))
                .param("month", String.valueOf(month))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.balance").value(5000));
    }
}
