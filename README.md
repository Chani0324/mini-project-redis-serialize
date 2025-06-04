| 구분                 | 측정 지표                                                                                          | 목적/효과                                              |
| ------------------ | ---------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| 순수 직렬화             | 1) 바이트 변환 시간(`serialize`) <br> 2) 바이트 → 객체 시간(`deserialize`)                                   | 직렬화 라이브러리 자체의 성능 차이를 명확하게 드러냄                      |
| 저장된 데이터 크기         | 1) `MEMORY USAGE key` <br> 2) `INFO memory` change                                             | 직렬화된 결과물이 Redis 안에서 차지하는 공간(메모리) 차이 분석             |
| 쓰기+읽기 (End‐to‐end) | 1) `SET` + `GET` 전체 응답 시간 <br> 2) 평균/백분위(latency percentiles) <br> 3) CPU 사용률 <br> 4) 네트워크 대역폭 | 실 서비스 시나리오(쓰기 후 곧바로 읽기)에서 직렬화 방식별 진짜 체감 차이를 분석     |
| Redis I/O만 비교      | 1) 단순 `SET`/`GET` 반복 <br> 2) 벤치 도구(redis-benchmark) 사용                                         | I/O 지연이 직렬화 비용에 비해 얼마나 큰 영향을 주는지 파악 (직렬화가 병목인지 파악) |


/**
     * 배치 저장 엔드포인트
     *
     * 요청 예시:
     * POST /api/v1/products/batch
     * Content-Type: application/json
     *
     * {
     *   "serializationType": "json",
     *   "threadCount": 10,
     *   "count": 1000
     * }
     *
     * - serializationType: "json", "gzip", "kryo", "protobuf", "msgpack" 중 하나
     * - threadCount: Redis 저장 시 사용할 스레드 수
     * - count: 생성해서 저장할 ProductDto 개수
     */
