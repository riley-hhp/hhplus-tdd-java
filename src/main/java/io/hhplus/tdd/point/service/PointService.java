package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PointService {

    /**
     * 특정 유저의 포인트를 조회하는 기능
     */
    public default UserPoint selectById(long id) {
        return new UserPoint(0, 0, 0);
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회하는 기능
     */
    public default List<PointHistory> selectAllByUserId(long id) {
        return List.of();
    }

    /**
     * 특정 유저의 포인트를 충전하는 기능
     */
    public default UserPoint charge(long id, long amount) {
        return new UserPoint(0, 0, 0);
    }

    /**
     * 특정 유저의 포인트를 사용하는 기능
     */
    public default UserPoint use(long id, long amount) {
        return new UserPoint(0, 0, 0);
    }
}
