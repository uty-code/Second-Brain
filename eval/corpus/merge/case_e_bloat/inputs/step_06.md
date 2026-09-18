## 논쟁: Redlock의 시스템 시계 편차 한계
Martin Kleppmann은 Redlock이 시스템 시계(Clock Drift) 및 긴 GC 정지(Stop-the-world)에 취약하다고 지적했습니다. 강력한 정확성이 필요한 시스템이라면 펜싱 토큰(Fencing Token)이나 ZooKeeper 같은 합의 엔진이 더 적합할 수 있습니다.
