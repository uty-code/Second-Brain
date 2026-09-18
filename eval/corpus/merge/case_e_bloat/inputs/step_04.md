## 파라미터: waitTime과 leaseTime
Redisson 락 획득 메서드(tryLock) 호출 시 waitTime(락 대기 최대 시간)과 leaseTime(락 자동 반납 만료 시간)을 도메인 특성에 맞게 설정해야 합니다. leaseTime을 명시하면 와치독이 동작하지 않으므로 주의가 필요합니다.
