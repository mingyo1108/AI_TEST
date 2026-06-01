# shop-advanced — Claude Code 심화과정 실습 저장소

식품 온라인몰의 재고 백오피스를 단순화한 실습용 저장소입니다. 내용은 모두 가상입니다.
프로젝트 설명과 규칙은 [`CLAUDE.md`](CLAUDE.md)를 봐 주세요.

- Java 25, Spring Boot 4.1, MyBatis, Gradle 9 (wrapper 포함), H2 (MSSQL 호환 모드)
- JDK 25가 없으면 Gradle이 처음 빌드할 때 자동으로 내려받습니다 (사내망에서 막히면 JDK 25를 직접 설치).
- **Windows: Git for Windows 설치 필수** (Claude Code와 Git hook이 내부에서 Git Bash를 씁니다).
- 실습의 [터미널] 명령은 OS마다 다르면 **macOS**, **Windows (PowerShell)**, **Windows (CMD)** 세 가지로 적어 두었습니다. Windows는 VS Code 터미널 오른쪽 위 목록에서 터미널 종류(powershell, cmd)를 보고 맞는 명령을 입력합니다. Git Bash를 쓰면 macOS 명령을 씁니다.
- Claude Code는 네이티브 설치를 권장합니다 (`claude --version`으로 확인).
- Node.js 20 이상 (DB 도구·SQL 렌더러 스크립트, 3번 모듈 Excel MCP, 4번 모듈 Hook 스크립트)

## 수업 전 준비

macOS
```bash
git clone <수강생용 저장소 주소> shop-advanced
cd shop-advanced
./gradlew build            # 의존성 내려받기 + 테스트 + 린트
cp .env.example .env       # DB 접속 정보 (커밋하지 않음)
./gradlew dbReset          # 개발 DB·운영 흉내 DB 만들기 (.local/db)
node scripts/db.mjs --env dev "SELECT COUNT(*) FROM stock_lot WITH (NOLOCK)"   # 숫자가 나오면 준비 완료
```

Windows (PowerShell)
```powershell
git clone <수강생용 저장소 주소> shop-advanced
cd shop-advanced
.\gradlew.bat build                 # 의존성 내려받기 + 테스트 + 린트
Copy-Item .env.example .env         # DB 접속 정보 (커밋하지 않음)
.\gradlew.bat dbReset               # 개발 DB·운영 흉내 DB 만들기 (.local/db)
node scripts/db.mjs --env dev "SELECT COUNT(*) FROM stock_lot WITH (NOLOCK)"   # 숫자가 나오면 준비 완료
```

Windows (CMD)
```bat
git clone <수강생용 저장소 주소> shop-advanced
cd shop-advanced
gradlew.bat build
copy .env.example .env
gradlew.bat dbReset
node scripts/db.mjs --env dev "SELECT COUNT(*) FROM stock_lot WITH (NOLOCK)"
```

## 내 GitHub 저장소 연결 (처음 한 번)

실습 결과는 각자 GitHub 저장소에 올려 보관합니다. 클론한 저장소(`origin`)의 이름을 `upstream`으로 바꾸고, 내 저장소를 `origin`으로 연결합니다.

GitHub CLI(`gh`)가 있으면:

```bash
gh auth login                                   # 처음 한 번
git remote rename origin upstream
gh repo create shop-advanced --private --source=. --remote=origin
git push origin main --tags
```

GitHub CLI가 없으면 GitHub 웹에서 빈 저장소를 만들고(README·.gitignore 추가하지 않음):

```bash
git remote rename origin upstream
git remote add origin https://github.com/<내 계정>/shop-advanced.git
git push origin main --tags
```

`git remote -v`로 `origin`이 내 저장소, `upstream`이 수강생용 저장소인지 확인합니다.
GitHub에 접근할 수 없는 환경이면 이 단계는 건너뛰고 로컬에 커밋만 해 둡니다. 나중에 연결해 한 번에 올리면 됩니다.

## 모듈별 실습 흐름

모듈마다 정해진 시작 태그가 있습니다. 앞 모듈 결과와 상관없이, 모듈을 시작할 때마다 **시작 태그에서 내 실습 브랜치를 새로 만들어** 진행합니다. `main`과 태그는 건드리지 않습니다.

```bash
# 1. 이전 모듈 작업을 내 브랜치에 커밋 (변경이 없으면 건너뜀)
git add -A
git commit -m "chore: 3번 모듈 실습 결과"

# 2. 시작 태그에서 실습 브랜치 만들기
git switch -c practice/m4 m4-start

# 3. 실습 (Claude Code) → 단계마다 커밋

# 4. 내 원격 저장소에 올리기 (모듈마다, 또는 마지막에 한 번)
git push -u origin practice/m4
```

| 모듈 | 시작 태그 | 실습 브랜치 만들기 |
|---|---|---|
| 2 실습 환경 세팅 & 코드베이스 파악 | `m2-start` | `git switch -c practice/m2 m2-start` |
| 3 사람의 요구사항을 AI-Ready Spec으로 구조화하기 | `m3-start` | `git switch -c practice/m3 m3-start` |
| 4 Agentic Development Workflow 자동화 | `m4-start` | `git switch -c practice/m4 m4-start` |
| 5 Subagent를 활용한 전문 작업 위임 | `m5-start` | `git switch -c practice/m5 m5-start` |
| 6 Multi-Agent 기반 대규모 이슈 분석 및 해결 | `m6-start` | `git switch -c practice/m6 m6-start` |

1번 모듈(Claude Code와 Agentic Coding 이해)은 이 저장소를 쓰지 않습니다.

마지막에 한 번에 올릴 때는 `git push origin --all` 로 실습 브랜치를 모두 올립니다.

### 4번에서 늦어졌을 때

지금까지 한 작업을 커밋해 두고, 복구 태그에서 새 브랜치를 만들어 다음 단계에 합류합니다. 내 작업은 원래 브랜치에 그대로 남습니다.

```bash
git add -A
git commit -m "chore: 4번 진행 중"
git switch -c practice/m4-task1 m4-task1-done   # 첫 번째 API 구현 완료 → Skill 제작부터
git switch -c practice/m4-task2 m4-task2-done   # 또는: add-api Skill 제작 완료 → Skill 재사용·Hook부터
```

### 5번에서 늦어졌을 때

```bash
git add -A
git commit -m "chore: 5번 진행 중"
git switch -c practice/m5-fixed m5-dsql-fixed   # 동적 SQL 수정·케이스 테스트 완료 → 4단계(mapper-reviewer)부터
```

### 자주 막히는 경우

| 증상 | 해결 |
|---|---|
| 브랜치를 만들 때 "would be overwritten" 오류 | 커밋하지 않은 변경이 있습니다. 위 1번(커밋)을 먼저 합니다 |
| `practice/m4 already exists` | 같은 이름의 브랜치가 이미 있습니다. 이어서 하려면 `git switch practice/m4`, 새로 시작하려면 `practice/m4-2` 같은 다른 이름을 씁니다 |
| 지금 어느 브랜치인지 모르겠음 | `git status` 첫 줄, 또는 `git branch` |
| Windows에서 `./gradlew`·`tail`·`chmod` 등이 실패 | macOS 명령을 입력한 경우입니다. 실습 프롬프트에서 지금 터미널(PowerShell 또는 CMD)에 맞는 Windows 블록을 씁니다 |

## 실습 프롬프트

수업 중 입력할 프롬프트와 명령은 `prompts/`에 모듈별로 있습니다. 모듈 시작 지점으로 이동하면 그 모듈까지의 파일이 들어 있습니다.

- 파일을 편집기에서 열어 코드 블록을 그대로 복사해 붙여넣습니다. `<꺾쇠>` 부분만 자기 값으로 바꿉니다.
- Claude Code에 `@prompts/...`로 파일을 붙이지 말고, 내용을 복사해 입력합니다.
- Claude Code가 이 폴더를 읽거나 검색하지 않도록 `.claude/settings.json`(읽기 차단)과 `.ignore`(내용 검색 제외)로 막아 두었습니다. Claude의 결과가 프롬프트 내용에 끌려가지 않게 하려는 것이니 지우지 마세요.

## 자주 쓰는 명령

Windows에서는 `./gradlew`를 `.\gradlew.bat`(PowerShell) 또는 `gradlew.bat`(CMD)으로 바꿔 입력합니다. 나머지 명령은 모두 같습니다.

| 할 일 | 명령 |
|---|---|
| 빌드 (테스트 + 린트) | `./gradlew build` |
| 전체 테스트 | `./gradlew test` |
| 린트 | `./gradlew checkstyleMain` |
| 실행 | `./gradlew bootRun` |
| DB 다시 만들기 | `./gradlew dbReset` |
| SQL 실행 (개발 DB) | `node scripts/db.mjs --env dev "SELECT ..."` |
| 현재 브랜치·변경 확인 | `git status` |
| 내 실습 브랜치 목록 | `git branch` |
| 내 저장소에 올리기 | `git push -u origin <브랜치>` / 전부: `git push origin --all` |
