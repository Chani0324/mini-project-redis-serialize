| 구분                 | 측정 지표                                                                                          | 목적/효과                                              |
| ------------------ | ---------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| 순수 직렬화             | 1) 바이트 변환 시간(`serialize`) <br> 2) 바이트 → 객체 시간(`deserialize`)                                   | 직렬화 라이브러리 자체의 성능 차이를 명확하게 드러냄                      |
| 저장된 데이터 크기         | 1) `MEMORY USAGE key` <br> 2) `INFO memory` change                                             | 직렬화된 결과물이 Redis 안에서 차지하는 공간(메모리) 차이 분석             |
| 쓰기+읽기 (End‐to‐end) | 1) `SET` + `GET` 전체 응답 시간 <br> 2) 평균/백분위(latency percentiles) <br> 3) 반환시간 <br> | 서비스 시나리오(쓰기 후 곧바로 읽기)에서 직렬화 방식별 진짜 체감 차이를 분석     |



## Batch 저장 엔드포인트

**Endpoint:** `POST /benchmark`  
**Content-Type:** `application/json`

### 요청 예시
```json

{
     "serializationType": "json",
     "threadCount": 6,
     "count": 1000
}
```
**"serializationType"** : "json", "gzip", "kryo", "protobuf", "msgpack"
**"threadCount"** : 양수, 최대 6
**"count"** : 양수, 최대 2000

### 응답 예시:
```json
     {
        "data": {
          "serializationType": "json",
          "threadCount": 6,
          "count": 1000,
          "saveElapsedMillis": 150,
          "readElapsedMillis": 80,
          "totalMemoryBytes": 512000
        }
     }
```

## 프로젝트 한계점
- 스레드 풀 매 요청 생성/해제
     - 매번 요청 시 Executors.newFixedThreadPool(threadCount)로 새 풀을 만들고 종료하기 때문에, 스레드 생성·소멸 오버헤드가 큼.
- 블로킹 대기(CountDownLatch.await)
     - 모든 작업이 끝날 때까지 메인 스레드가 블로킹되므로, 부분 완료 시점이나 예외 처리 후 신속 대응이 어려움
     -> 원래 프로젝트 진행 목적이 직렬화와 스레드 수에 따른 차이만 보려고 했으니 괜찮다고 생각함.
