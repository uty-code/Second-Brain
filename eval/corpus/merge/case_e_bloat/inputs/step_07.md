## 스프링 연동: AOP 기반 어노테이션 패턴
Spring Boot에서는 비즈니스 코드 침투를 방지하기 위해 `@DistributedLock(key = "#orderId")` 커스텀 어노테이션과 AspectJ AOP를 구현하여 선언적으로 락을 획득/해제하는 방식을 널리 활용합니다.
