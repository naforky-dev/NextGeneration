# NextGeneration
한정된 크기의 월드에서 가장 먼저 `[그다음 세대]` 발전 과제를 깨는 플레이어가 승리하는 플러그인

>[!IMPORTANT]
> 이 플러그인을 유튜브와 같은 영상 플랫폼에 사용할 경우 플러그인 링크와 다음의 출처를 남겨주세요.
>
> 제작자: 나포키(naforky-dev)


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
