package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.constant.ErrorMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final UserPointTable userPointRepository;
    private final PointHistoryTable pointHistoryRepository;

    private final ConcurrentHashMap<String, Lock> userLockMap = new ConcurrentHashMap<>();

    private Lock getUserLock(long userid) {

        return userLockMap.computeIfAbsent(String.valueOf(userid), id -> new ReentrantLock());
    }

    @Override
    public UserPoint selectById(long id) {

        validateId(id);
        return userPointRepository.selectById(id);
    }

    @Override
    public List<PointHistory> selectAllByUserId(long id) {

        validateId(id);
        return pointHistoryRepository.selectAllByUserId(id);
    }

    @Override
    public UserPoint charge(long id, long chargeAmount) {

        validateId(id);
        return executePointTransactionWithUserLock(id, chargeAmount, TransactionType.CHARGE);
    }

    @Override
    public UserPoint use(long id, long useAmount) {

        validateId(id);
        getCurrentPointOrThrow(id);
        return executePointTransactionWithUserLock(id, useAmount, TransactionType.USE);
    }

    /**
     * 포인트 충전/차감을 계산합니다.
     *
     * @param id
     * @param amount
     * @param transactionType
     * @return
     */
    public long calculatePoint(long id, long userPoint, long amount, TransactionType transactionType) {

        if(transactionType!=null){

            if (transactionType.equals(TransactionType.CHARGE)) {
                return userPoint + amount;
            }
            if (transactionType.equals(TransactionType.USE)) {
                if ( amount > userPoint ) {
                    throw new RuntimeException(ErrorMessage.INSUFFICIENT_BALANCE.format(id, userPoint));
                }
                return userPoint - amount;
            }
        }
        throw new IllegalArgumentException(ErrorMessage.IN_VALID_TRANSACTION_TYPE.format(transactionType));
    }

    /**
     * 포인트 트랜젝션
     *
     * @param id
     * @param deltaPoint
     * @param transactionType
     * @return
     */
    private UserPoint pointTransaction(long id, long deltaPoint, TransactionType transactionType) {

        // 1. 현재 사용자 포인트 가져오기
        long currentPoint = getUserPointOrDefault(id);
        log.debug("거래 유형 = {}, 현재 포인트 = {}, 변동 포인트 = {}", transactionType, currentPoint, deltaPoint);

        // 2. 유효성 검사
        if (deltaPoint <=0) {
            throw new IllegalArgumentException(ErrorMessage.NEGATIVE_AMOUNT_ERROR.format(deltaPoint));
        }

        // 3. 포인트 계산
        long calculatedPoint = calculatePoint(id, currentPoint, deltaPoint, transactionType);
        log.debug("계산된 포인트 = {}", calculatedPoint);

        // 4. 사용자 포인트 업데이트 및 히스토리 기록
        UserPoint updatedPoint = userPointRepository.insertOrUpdate(id, calculatedPoint);
        pointHistoryRepository.insert(id, deltaPoint, transactionType, System.currentTimeMillis());

        return updatedPoint;
    }


    /**
     *  포인트 트랜젝션을 실행합니다.
     *
     * @param id
     * @param deltaPoint
     * @param transactionType
     * @return
     */
    private UserPoint executePointTransactionWithUserLock(long id, long deltaPoint, TransactionType transactionType) {

        return executeWithUserLock(id, () -> pointTransaction(id, deltaPoint, transactionType) );
    }

    /**
     * 유저 락을 사용하여 동시성 문제를 해결하며,
     * 포인트 작업을 실행합니다,
     *
     * @param userid
     * @param userPointSupplier
     * @return UserPoint
     */
    private UserPoint executeWithUserLock(long userid, Supplier<UserPoint> userPointSupplier) {

        Lock userLock = getUserLock(userid);
        userLock.lock(); // 락을 걸고
        log.debug("{} 사용자의 락을 걸었습니다. 시간: {}", userid, System.currentTimeMillis());
        try {
            return userPointSupplier.get();
        }
        catch (Exception e) {
            log.error("사용자 ID {}에 대한 트랜잭션 처리 중 오류 발생: {}", userid, e.getMessage());
            throw e;
        }
        finally {
            userLock.unlock(); // 락 해제
            log.debug("{} 사용자의 락을 해제했습니다. 시간: {}", userid, System.currentTimeMillis());
        }
    }

    /**
     * 사용자 포인트를 조회
     * 없으면 예외를 던집니다.
     *
     * @param id
     * @return
     */
    private long getCurrentPointOrThrow(long id) {

        return Optional.ofNullable(selectById(id))
                       .map(UserPoint::point)
                       .orElseThrow(() -> new RuntimeException(ErrorMessage.POINT_NOT_FOUND.format(id)));
    }

    /**
     * 사용자 포인트를 조회
     * 없으면 기본값을 반환합니다.
     *
     * @param id
     * @return
     */
    private long getUserPointOrDefault(long id) {

        return Optional.ofNullable(selectById(id)).orElse(UserPoint.empty(id)).point();
    }

    /**
     * 음수 아이디 입력을 방어합니다.
     * @param id
     */
    private void validateId(long id) {

        if (id<=0) {
            throw new IllegalArgumentException(ErrorMessage.NEGATIVE_ID_ERROR.format(id));
        }
    }

}
