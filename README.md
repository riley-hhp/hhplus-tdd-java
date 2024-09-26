## 동시성 제어
   ### synchronized와 Lock 비교

동시성 처리를 구현하는 방법에는 여러 가지가 있으며, 그중 두 가지는 synchronized 키워드와 Lock 인터페이스입니다. 이 두 가지 접근 방식은 각기 다른 특징과 장단점을 가지고 있습니다.

1. synchronized

   •	기본 개념: synchronized는 메서드나 블록에 적용되어 해당 코드 블록이 동시에 하나의 스레드만 실행될 수 있도록 보장합니다.

   •	간편함: 사용하기 쉽고 직관적이며, 코드가 간결하게 유지됩니다. 자바에서 내장된 키워드이기 때문에 추가적인 객체를 생성할 필요가 없습니다.
   
   •	상태 자동 관리: synchronized는 스레드가 메서드 실행을 마치면 자동으로 락을 해제합니다. 따라서 개발자는 명시적으로 락 해제를 신경 쓸 필요가 없습니다.
   
   •	Reentrancy: 동일한 스레드가 이미 잠금된 메서드나 블록에 재진입할 수 있도록 지원합니다.

   (단점)

	•	유연성 부족: synchronized는 더 이상 블록 중단 및 타임아웃과 같은 유연한 락 관리 기능을 제공하지 않습니다.
	
    •	성능: 많은 스레드가 동시에 접근하려 할 경우, 대기 시간이 길어질 수 있습니다. 이는 성능 저하를 유발할 수 있습니다.

2. Lock

   •	기본 개념: Lock은 자바의 java.util.concurrent.locks 패키지에 포함되어 있으며, 명시적으로 락을 관리할 수 있는 기능을 제공합니다.
   
   •	유연성: Lock은 여러 가지 락을 지원하며, tryLock()과 같은 메서드를 통해 블록이 아닌 방식으로 락을 시도할 수 있습니다. 이로 인해 스레드는 필요에 따라 락을 시도하고, 특정 시간 동안만 대기하게 할 수 있습니다.
   
   •	상태 관리: Lock을 사용하면 개발자가 락 해제를 명시적으로 처리해야 합니다. 이는 코드가 복잡해질 수 있지만, 더 많은 제어를 제공합니다.

   (단점)

    •	복잡성: Lock은 더 많은 코드와 관리가 필요합니다. 명시적으로 락을 해제하지 않으면 데드락이 발생할 수 있습니다.
	
    •	재진입: 기본적으로 Lock은 재진입을 지원하지 않습니다. 동일한 스레드가 락을 획득하려고 할 때, 스레드가 차단될 수 있습니다.


	synchronized는 간편하고 기본적인 동기화 메커니즘을 제공하지만, 
    복잡한 동시성 요구사항이 있는 경우 Lock을 사용하는 것이 더 나을 수 있습니다. 
    선택은 요구사항, 성능 및 코드의 복잡성에 따라 달라질 수 있습니다.

---


### 과제를 풀기 위해 사용한 동시성 구현 방법

1. 사용자 Locking

   •	ReentrantLock을 사용하여 각 사용자에 대한 독립적인 락을 관리합니다. ConcurrentHashMap을 통해 사용자 ID에 해당하는 락을 저장하고, 필요할 때마다 락을 획득합니다.
   
   •	포인트 충전 및 차감 작업을 수행할 때, 해당 사용자에 대한 락을 획득하여 동시에 여러 사용자가 동일한 포인트를 변경하지 않도록 합니다.


2. 트랜잭션 처리

   •	포인트 트랜잭션은 executeWithUserLock 메서드를 통해 처리됩니다. 이 메서드는 사용자 ID와 포인트 작업을 인수로 받아 락을 걸고 작업을 실행합니다.
   
   •	작업이 완료되면 락이 해제되며, 오류가 발생한 경우 로그를 기록하고 예외를 다시 던집니다.


---


### 동시성 테스트

1. 포인트 충전 동시성 테스트
    
    • 300개의 스레드를 생성하여 각 스레드가 100 포인트를 충전합니다. 모든 스레드가 완료된 후, 최종 포인트가 예상대로 충전되었는지 검증합니다.
 ```  
   void charge_동시성_테스트_CompletableFuture() throws InterruptedException, ExecutionException {

        int threadCount = 300;
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
   ```

2. 포인트 차감 동시성 테스트

    •	100개의 스레드를 생성하여 각 스레드가 100 포인트를 차감합니다. 초기 포인트가 충분히 설정된 상태에서 모든 스레드가 완료된 후, 최종 포인트가 0이 되는지 검증합니다.
  ``` 
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
    }sertions.assertEquals(0, userPoint.point());
   }
   ```