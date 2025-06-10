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

---

### ✅ 1. JSON (JavaScript Object Notation)

* **원리:**
  텍스트 기반의 key-value 쌍 구조. 사람이 읽기 쉽게 설계됨.
  객체를 문자열로 직렬화하고, 문자열을 다시 객체로 역직렬화함.
* **특징:**

  * 가독성 좋음 (human-readable)
  * 구조화된 데이터 표현에 적합
  * **크기가 큼** (불필요한 공백, 키 문자열 반복 등)
  * **압축 없음**, 이진 아님 → 느리고 무거움

---

### ✅ 2. GZIP (GNU zip)

* **원리:**
  LZ77 알고리즘 + Huffman 코딩으로 데이터의 **패턴을 찾아 압축**
  (중복된 문자열을 참조하고, 빈도 높은 데이터를 짧은 비트로 표현)
* **특징:**

  * **압축 포맷**이지 직렬화 포맷이 아님 → 보통 JSON 등과 **조합 사용**
  * 텍스트든 이진이든 압축 가능
  * CPU 사용 많음 (압축/해제 비용 있음)
  * 크기 절감률 높음, **느리지만 작게**

---

### ✅ 3. Kryo

* **원리:**
  Java 객체를 이진 형태로 빠르게 직렬화하기 위해 설계
  필드 오더, 타입 정보 최적화 → 객체 구조에 따라 **이진 표현을 최소화**
* **특징:**

  * **빠르고 작음**
  * Java 전용 최적화 → 자바 객체 그래프 순환, 레퍼런스 처리 가능
  * Reflection 최소화 (성능 개선)
  * **Schema 없음** → 구조 바뀌면 호환성 깨짐

---

### ✅ 4. Protobuf (Protocol Buffers)

* **원리:**
  Google에서 만든 **이진 포맷 기반 schema-driven serialization**
  `.proto` 파일로 구조 정의 → 고정된 필드 번호로 데이터 표현 (tag 기반)
* **특징:**

  * **크기 작고 속도 빠름**
  * schema 기반 → 명시적 구조 정의, **호환성 유지**
  * 다양한 언어 지원 (Java, Python, Go 등)
  * 디버깅 어려움 (바이너리이므로 사람이 읽기 어려움)

---

### ✅ 5. MessagePack (msgpack)

* **원리:**
  JSON처럼 key-value 구조를 유지하면서, 그걸 **바이너리로 변환**
  데이터 타입에 따라 최적 비트 수로 표현 (예: int8, int16 등)
* **특징:**

  * **JSON의 대체재**로, 구조 유사하되 **이진 포맷**
  * 크기 감소, 속도 개선
  * JSON ↔ MessagePack 변환 쉬움
  * 사람이 읽기 어려움 (이진 포맷이므로)

---

### 🔍 한눈에 비교 요약

| 방식          | 포맷   | 압축 | 특징 요약                  |
| ----------- | ---- | -- | ---------------------- |
| JSON        | 텍스트  | ❌  | 가독성 최고, 느리고 큼          |
| GZIP(JSON)  | 바이너리 | ✅  | 압축률 높음, 느림             |
| Kryo        | 바이너리 | ✅  | 빠름, Java 특화, schema 없음 |
| Protobuf    | 바이너리 | ✅  | 매우 빠름, schema 필요, 범용   |
| MessagePack | 바이너리 | ✅  | JSON 구조 유지 + 이진 최적화    |


