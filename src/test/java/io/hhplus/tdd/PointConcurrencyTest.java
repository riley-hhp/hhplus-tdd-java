package io.hhplus.tdd;

import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class PointConcurrencyTest {

    @Autowired
    private PointService pointService;


    @Test
    void charge_동시성_테스트_CompletableFuture() throws InterruptedException, ExecutionException {

        int threadCount = 200;
        int point = 100;

        // CompletableFuture 리스트 생성
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                pointService.charge(1L, point);
            });
            futures.add(future);
        }

        // 모든 CompletableFuture가 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // then
        UserPoint userPoint = pointService.selectById(1L);
        Assertions.assertEquals(threadCount * point, userPoint.point());
    }


    @Test
    void use_동시성_테스트_CompletableFuture() throws InterruptedException, ExecutionException {
        int threadCount = 100;
        long initialPoint = 10000; // 초기 포인트
        long usePoint = 100; // 차감할 포인트

        // 초기 포인트를 설정
        pointService.charge(1L, initialPoint);

        // CompletableFuture 리스트 생성
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                pointService.use(1L, usePoint);
            });
            futures.add(future);
        }

        // 모든 CompletableFuture가 완료될 때까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // then
        UserPoint userPoint = pointService.selectById(1L);
        long expectedPoint = initialPoint - (threadCount * usePoint); // 예상 포인트 계산
        Assertions.assertEquals(expectedPoint, userPoint.point());
    }
}