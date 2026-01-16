# NextGeneration
한정된 크기의 월드에서 가장 먼저 `[그다음 세대]` 발전 과제를 깨는 플레이어가 승리하는 플러그인

[![Build Plugin](https://github.com/naforky-dev/NextGeneration/actions/workflows/build.yml/badge.svg)](https://github.com/naforky-dev/NextGeneration/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

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
  - `reloadconfig` - `config.yml` 설정 다시 로드

`config.yml` - `/plugins/NextGen` 경로에 생성 가능한 설정 파일(서버 새로고침 시 설정 리셋 방지)

`/t` - 남은 엔드 활성화 시간 확인

### 설정 파일
>[!IMPORTANT]
>`config.yml` 파일을 아래의 방법 또는 구조와 다르게 생성할 경우 기본값이 로드됩니다.
>서버 로그에서 설정이 적용되었는지 확인할 수 있습니다.

```yml
# NextGen 플러그인 설정
nextgen-settings:
  # 모든 게임에서 생성되는 월드보더의 기본 크기(블록 단위).
  # 게임 내에서 /nextgen border <size>를 이용하여 임시로 변경 가능.
  default-border-size: 2000

  # 엔드 차원 활성화까지의 기본 시간.
  # 다음과 같은 포맷 사용 가능: 2h, 1h30m, 45m, 10m
  # 게임 내에서 /nextgen endactivationtime <format>를 이용하여 임시로 변경 가능.
  default-end-time: "2h"

  # true일 경우, 플레이어들이 포탈에 진입 시 아래의 용암에 빠져 사망할 수 있음.
  # false일 경우, 플레이어들이 용암에 빠졌을 때 10초 간 화염 저항 부여.
  # 게임 내에서 /nextgen portaldeath <true|false>를 이용하여 임시로 변경 가능.
  portal-death-on-entry: true
```

### 기본 옵션
>[!NOTE]
>아래의 설정은 위의 `config.yml` 파일이 없거나 잘못 작성된 경우
>플러그인이 기본적으로 로드하는 설정입니다.
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
  사망 시 리스폰 설정된 침대가 없는 경우 랜덤 위치 스폰됩니다.
  >[!NOTE]
  >랜덤 리스폰 할 적절한 위치를 찾지 못한 경우 월드 스폰에서 스폰됩니다.

---
> (c) 2025 [나포키(naforky)](https://youtube.com/@나포키), [naforky-dev](https://github.com/naforky-dev). All rights reserved.
