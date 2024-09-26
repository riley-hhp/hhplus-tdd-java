package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private PointService pointService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        pointService = Mockito.mock(PointService.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void point_특정_유저의_포인트를_조회한다() throws Exception {
        // 특정 유저의 포인트 데이터 설정
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        when(pointService.selectById(userId)).thenReturn(userPoint);

        // 요청 및 응답 검증
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                ;
    }

    @Test
    void history_특정_유저의_포인트_충전_이용_내역을_조회한다() throws Exception {
        // 특정 유저의 포인트 이력 데이터 설정
        long userId = 1L;
        List<PointHistory> histories = Collections.singletonList(new PointHistory(1, userId, 50L, TransactionType.CHARGE, System.currentTimeMillis()));
        when(pointService.selectAllByUserId(userId)).thenReturn(histories);

        // 요청 및 응답 검증
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
        ;
    }

    @Test
    void charge_특정_유저의_포인트를_충전한다() throws Exception {
        // 포인트 충전 데이터 설정
        long userId = 1L;
        long chargeAmount = 100L;
        UserPoint updatedPoint = new UserPoint(userId, 200L, System.currentTimeMillis());
        when(pointService.charge(userId, chargeAmount)).thenReturn(updatedPoint);

        // 요청 및 응답 검증
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
        ;
    }

    @Test
    void use_특정_유저의_포인트를_사용한다() throws Exception {
        // 포인트 사용 데이터 설정
        long userId = 1L;
        long useAmount = 50L;
        UserPoint updatedPoint = new UserPoint(userId, 150L, System.currentTimeMillis());
        when(pointService.use(userId, useAmount)).thenReturn(updatedPoint);

        // 요청 및 응답 검증
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().is5xxServerError())
        ;
    }
}