// 외부 명령(gradlew, claude, java, git)을 macOS·Linux·Windows에서 같은 방식으로 실행하는 유틸.
//
// Windows에서 Node 20.12 이상은 .bat·.cmd 파일을 셸 없이 실행하지 못한다(보안 패치).
// 그래서 .bat·.cmd(gradlew.bat, npm으로 설치한 claude.cmd)는 cmd.exe를 거쳐 실행하고,
// .exe(네이티브 설치 claude.exe, java.exe, git.exe)는 그대로 실행한다.
import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const IS_WINDOWS = process.platform === 'win32';

/** 저장소 루트 (scripts/lib/ 의 두 단계 위). */
export const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

/** PATH에서 실행 파일을 찾는다. Windows는 PATHEXT(.EXE, .CMD, .BAT …)를 붙여 본다. */
export function which(name) {
  if (name.includes('/') || name.includes('\\')) {
    return existsSync(name) ? name : null;
  }
  const dirs = (process.env.PATH || '').split(path.delimiter).filter(Boolean);
  const exts = IS_WINDOWS ? (process.env.PATHEXT || '.EXE;.CMD;.BAT').split(';') : [''];
  for (const dir of dirs) {
    for (const ext of exts) {
      const candidate = path.join(dir, name + ext.toLowerCase());
      if (existsSync(candidate)) return candidate;
      const upper = path.join(dir, name + ext);
      if (existsSync(upper)) return upper;
    }
  }
  return null;
}

/** cmd.exe 인자 한 개를 큰따옴표로 감싼다. */
function quoteForCmd(arg) {
  return '"' + String(arg).replace(/"/g, '""') + '"';
}

/**
 * 명령을 실행하고 { status, stdout, stderr } 를 돌려준다.
 * options: cwd(기본 저장소 루트), input(표준 입력 문자열), env, inherit(true면 출력을 그대로 터미널에)
 */
export function run(command, args = [], options = {}) {
  const file = which(command) || command;
  const spawnOptions = {
    cwd: options.cwd || ROOT,
    env: options.env || process.env,
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
    // 표준 입력을 비워 두지 않으면 claude -p 가 "no stdin data" 경고를 낸다
    input: options.input ?? '',
    stdio: options.inherit ? ['pipe', 'inherit', 'inherit'] : 'pipe',
  };
  let result;
  if (IS_WINDOWS && /\.(cmd|bat)$/i.test(file)) {
    const line = [file, ...args].map(quoteForCmd).join(' ');
    result = spawnSync(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', '"' + line + '"'], {
      ...spawnOptions,
      windowsVerbatimArguments: true,
    });
  } else {
    result = spawnSync(file, args, spawnOptions);
  }
  if (result.error) {
    return { status: 127, stdout: '', stderr: `${command} 를 실행하지 못했습니다: ${result.error.message}` };
  }
  return { status: result.status ?? 1, stdout: result.stdout || '', stderr: result.stderr || '' };
}

/** 저장소의 Gradle wrapper 를 실행한다 (gradlew / gradlew.bat 자동 선택). */
export function runGradle(args, options = {}) {
  const wrapper = path.join(ROOT, IS_WINDOWS ? 'gradlew.bat' : 'gradlew');
  return run(wrapper, args, options);
}

/** java 실행 파일. JAVA_HOME 이 있으면 그 안의 java 를 쓴다. */
export function javaCommand() {
  if (process.env.JAVA_HOME) {
    const candidate = path.join(process.env.JAVA_HOME, 'bin', IS_WINDOWS ? 'java.exe' : 'java');
    if (existsSync(candidate)) return candidate;
  }
  return 'java';
}

/** .env 를 읽어 객체로 돌려준다. 없으면 .env.example. 둘 다 없으면 빈 객체. */
export function readEnv() {
  for (const name of ['.env', '.env.example']) {
    const file = path.join(ROOT, name);
    if (existsSync(file)) {
      const env = {};
      for (const raw of readFileSync(file, 'utf8').split(/\r?\n/)) {
        const line = raw.trim();
        const eq = line.indexOf('=');
        if (!line || line.startsWith('#') || eq < 0) continue;
        env[line.slice(0, eq).trim()] = line.slice(eq + 1).trim().replace(/^(['"])(.*)\1$/, '$2');
      }
      return { file: name, values: env };
    }
  }
  return { file: null, values: {} };
}
