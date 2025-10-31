# NextGeneration
한정된 크기의 월드에서 가장 먼저 `[그다음 세대]` 발전 과제를 깨는 플레이어가 승리하는 플러그인

>[!IMPORTANT]
> 이 플러그인을 유튜브와 같은 영상 플랫폼에 사용할 경우 플러그인 링크와 다음의 출처를 남겨주세요.
>
> 아이디어: `나포키`
>
> 제작자: `naforky-dev`


### License
이 플러그인은 GNU의 GPL-3.0 라이선스 하에 배포됩니다.

### 실행 환경
PaperMC `1.21.8`

Java 25 `temurin`(Oracle의 [최신 Java](https://www.oracle.com/java/technologies/downloads/#java25) 버전)

### Usage
`/nextgen <command>`

`<command>`:
  - `start` - 게임 시작
  - `reload` - 서버 새로고침(문제 발생시)
  - `abort` - 게임 중단
  - `border <n>` - 월드보더 크기 변경
  - `showtimer <true|false>` - 엔드 활성화 타이머
  - `endactivationtime <format>` - 엔드 활성화 시간
  - `portaldeath <true|false>` - 엔드 포탈 사망 설정

`/t` - 남은 엔드 활성화 시간 확인

### 기본 옵션
```yml
border: 1500
showtimer: false
endactivationtime: 2h
portaldeath: true
```

### 명령어 설명
`border <n>`:
  - `n`에 숫자 형식으로 원하는 월드보더의 크기를 입력
  - 예시: `/nextgen border 1500`(월드보더 크기 `1500x1500`)
`showtimer <true|false>`:
  - 설정값이 `true`일 경우 게임이 실행되는 동안 엔드 활성화 보스바 표시
`endactivationtime <format>`:
  - `format`에 `1h2m3s`형식으로 엔드 활성화까지의 시간 설정
  - 예시: `/nextgen endactivationtime 2h0m15s`(엔드 활성화까지 `2시간 0분 15초`)
`portaldeath <true|false>`:
  - 설정값이 `false`일 경우 엔드 포탈 진입 시 엔드가 비활성화된 경우 사망 방지(화염 저항 10초)

### 특수 규칙
  - 사망 시 리스폰 설정된 침대가 없는 경우 랜덤 위치 스폰
  >[!NOTE]
  >랜덤 리스폰 할 적절한 위치를 찾지 못한 경우 월드 스폰에서 스폰

---
> (c) 2025 [나포키(naforky)](https://youtube.com/@나포키), [naforky-dev](https://github.com/naforky-dev). All rights reserved.
