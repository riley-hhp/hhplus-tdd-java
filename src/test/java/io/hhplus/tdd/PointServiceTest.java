package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.service.PointServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 포인트 서비스 단위 테스트
 *
 */
@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointServiceImpl pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;


    @Test
    public void 음수_아이디_테스트() {
        long invalidUserId = -1L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pointService.selectById(invalidUserId);
        });
        assertEquals("음수 아이디는 사용할 수 없습니다. id: -1", exception.getMessage());
    }

    @Test
    public void 충전_불가_금액_테스트() {
        long userId = 1L;
        long invalidAmount = -100L;  // 유효하지 않은 금액
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pointService.charge(userId, invalidAmount);
        });
        assertEquals("충전할 수 없는 금액입니다. amount: -100", exception.getMessage());
    }

    @Test
    public void 잔액_부족_테스트() {
        long userId = 1L;
        long useAmount = 1000L;  // 잔액보다 큰 금액을 사용하려고 함
        long currentBalance = 500L;

        // 잔액을 500으로 설정
        when(pointService.selectById(userId)).thenReturn(new UserPoint(userId, currentBalance, System.currentTimeMillis()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pointService.use(userId, useAmount);
        });
        assertEquals("사용자의 포인트 잔액이 부족합니다. id: 1, 잔액: 500", exception.getMessage());
    }

    @Test
    void 유효하지_않은_거래유형_테스트() {
        long userId = 1L;
        long amount = 100L;
        TransactionType invalidType = null;  // 잘못된 거래 유형

        RuntimeException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.calculatePoint(userId, 0, amount, invalidType);
        });
        assertEquals("유효하지 않은 거래유형 입니다. transactionType: null", exception.getMessage());
    }

    @Test
    public void 포인트_조회_테스트() {

        // given
        long userId = 1L;
        long initialPoint = 500L;
        long time = System.currentTimeMillis();

        UserPoint userPoint = new UserPoint(userId, 0, time);
        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        //when
        UserPoint result = pointService.selectById(userId);

        //then
        assertEquals(0, result.point());
        verify(userPointTable).selectById(userId);
    }

    @Test
    public void 포인트_음수_충전_테스트() {

        // given
        long userId = 1L;
        long charge = -500L;

        // when, then
        assertThrows(IllegalArgumentException.class, () -> pointService.charge(userId,charge));
    }

    @Test
    public void 포인트_충전_테스트() {

        // given
        long userId = 1L;
        long initialPoint = 500L;
        long chargeAmount = 200L;
        long time = System.currentTimeMillis();

        // 기존 포인트 500 설정
        UserPoint existingPoint = new UserPoint(userId, initialPoint, time);
        when(userPointTable.selectById(userId)).thenReturn(existingPoint);

        // 충전 후의 포인트 설정
        UserPoint chargedUserPoint = new UserPoint(userId, (initialPoint + chargeAmount), time);
        when(userPointTable.insertOrUpdate(userId, (initialPoint + chargeAmount))).thenReturn(chargedUserPoint);

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);
        System.out.println("result = " + result);

        UserPoint point = pointService.selectById(userId);
        System.out.println("point = " + point);

        // then
        assertEquals(700L, result.point()); // 충전 후의 총 포인트가 700인지 확인
    }


    // 사용
    @Test
    public void 포인트_초과_사용_테스트() {

        // given
        long userId = 1L;
        long initialPoint = 500L;
        long useAmount = 501L;
        long time = System.currentTimeMillis(); // 고정된 시간 값

        // when
        // then
        assertThrows(RuntimeException.class, () -> pointService.use(userId, useAmount));
    }

    @Test
    public void 포인트_사용_테스트() {
        // given
        long userId = 1L;
        long initialPoint = 500L;
        long useAmount = 200L;

        // Mock: 초기 포인트 설정
        UserPoint existingPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(existingPoint);

        // Mock: 포인트 사용 후 포인트 업데이트
        UserPoint updatedPoint = new UserPoint(userId, initialPoint - useAmount, System.currentTimeMillis());
        when(userPointTable.insertOrUpdate(userId, initialPoint - useAmount)).thenReturn(updatedPoint);

        // when
        UserPoint result = pointService.use(userId, useAmount);
        System.out.println("result = " + result);
        // then
        assertEquals(300L, result.point()); // 사용 후의 총 포인트가 300인지 확인
    }


    // 이력
    @Test
    public void 포인트_이력_확인_테스트() {

        // given
        long userId = 1L;
        long charge1 = 500L;
        long charge2 = 200L;
        long charge3 = 300L;

        long time1 = 1727090971000L;
        long time2 = 1727090972000L;
        long time3 = 1727090973000L;

        // 첫 번째 충전 후 포인트
        UserPoint chargePoint1 = new UserPoint(userId, charge1, time1);
        when(userPointTable.insertOrUpdate(userId, charge1)).thenReturn(chargePoint1);

        // 두 번째 충전 후 포인트
        UserPoint chargePoint2 = new UserPoint(userId, charge2, time2);
        when(userPointTable.insertOrUpdate(userId, charge2)).thenReturn(chargePoint2);

        // 세 번째 충전 후 포인트
        UserPoint chargePoint3 = new UserPoint(userId, charge3, time3);
        when(userPointTable.insertOrUpdate(userId, charge3)).thenReturn(chargePoint3);

        // 포인트 이력 설정
        List<PointHistory> pointHistoryList = List.of(
                new PointHistory(1, userId, charge1, TransactionType.CHARGE, time1),
                new PointHistory(2, userId, charge2, TransactionType.CHARGE, time2),
                new PointHistory(3, userId, charge3, TransactionType.CHARGE, time3)
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistoryList);

        // when
        pointService.charge(userId, charge1);
        pointService.charge(userId, charge2);
        pointService.charge(userId, charge3);

        List<PointHistory> resultHistory = pointService.selectAllByUserId(userId); // 이력 조회

        // then
        assertEquals(3, resultHistory.size()); // 세 번의 충전 이력이 쌓였는지 확인
        assertEquals(charge1, resultHistory.get(0).amount());
        assertEquals(charge2, resultHistory.get(1).amount());
        assertEquals(charge3, resultHistory.get(2).amount());
    }


}
