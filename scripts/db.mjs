#!/usr/bin/env node
// DB 도구: AI와 사람이 SQL을 실행하는 단 하나의 통로.
//
//   node scripts/db.mjs --env dev "SELECT lot_no, status FROM stock_lot WITH (NOLOCK)"
//   node scripts/db.mjs --env prod --file query.sql
//
// - 접속 정보는 .env 에서 읽는다 (채팅에 계정을 입력하지 않는다). 없으면 .env.example.
// - 결과 표와 DB 실행 시간(JVM 기동 시간 제외)을 출력한다.
// - 실행한 SQL·환경·결과를 logs/sql-audit.log 에 남긴다 (실패한 실행도 남는다).
// - DB 파일과 실행기는 ./gradlew dbReset 으로 준비한다.
import { appendFileSync, existsSync, mkdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { ROOT, javaCommand, readEnv, run } from './lib/run.mjs';

const USAGE = '사용법: node scripts/db.mjs --env dev|prod "<SQL>"  |  node scripts/db.mjs --env dev|prod --file <sql 파일>';

const args = process.argv.slice(2);
let envName = null;
let file = null;
let sql = null;
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--env') envName = args[++i];
  else if (args[i] === '--file') file = args[++i];
  else if (sql === null) sql = args[i];
  else {
    console.error('SQL은 따옴표로 감싼 인자 하나로 주세요.\n' + USAGE);
    process.exit(2);
  }
}
if (!['dev', 'prod'].includes(envName) || (sql === null) === (file === null)) {
  console.error(USAGE);
  process.exit(2);
}
if (file !== null) {
  const resolved = path.resolve(process.cwd(), file);
  if (!existsSync(resolved)) {
    console.error(`SQL 파일이 없습니다: ${file}`);
    process.exit(2);
  }
  file = resolved;
}

const libDir = path.join(ROOT, '.local', 'lib');
const dbFile = path.join(ROOT, '.local', 'db', `${envName}.mv.db`);
if (!existsSync(libDir) || !existsSync(dbFile)) {
  console.error('DB가 준비되지 않았습니다. 먼저 ./gradlew dbReset 을 실행하세요.');
  process.exit(2);
}

const { values } = readEnv();
const prefix = envName === 'dev' ? 'DB_DEV_' : 'DB_PROD_';
const url = (values[prefix + 'URL'] || `jdbc:h2:file:./.local/db/${envName};MODE=MSSQLServer`) + ';IFEXISTS=TRUE';
const user = values[prefix + 'USER'];
if (!user) {
  console.error(`.env 에 ${prefix}USER 가 없습니다. cp .env.example .env 를 먼저 실행하세요.`);
  process.exit(2);
}

const classpath = path.join('.local', 'lib', '*');
const javaArgs = ['-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8',
  '-cp', classpath, 'shoptools.DbCli', ...(file !== null ? ['--file', file] : [sql])];
const result = run(javaCommand(), javaArgs, {
  env: { ...process.env, DB_URL: url, DB_USER: user, DB_PASSWORD: values[prefix + 'PASSWORD'] || '' },
});
process.stdout.write(result.stdout);
process.stderr.write(result.stderr);

// 감사 로그: 시각 | 환경 | 계정 | 결과 | SQL(한 줄로)
const text = file !== null ? readFileSync(file, 'utf8') : sql;
const oneLine = text.replace(/\s+/g, ' ').trim().slice(0, 1000);
const outcome = result.status === 0 ? 'OK' : 'ERROR';
const firstError = result.status === 0 ? '' : ' | ' + (result.stderr.split(/\r?\n/).find((l) => l.startsWith('오류')) || '').slice(0, 200);
try {
  mkdirSync(path.join(ROOT, 'logs'), { recursive: true });
  appendFileSync(path.join(ROOT, 'logs', 'sql-audit.log'),
    `${localTimestamp()} | env=${envName} | user=${user} | ${outcome}${firstError} | ${oneLine}\n`);
} catch (e) {
  console.error(`(감사 로그를 남기지 못했습니다: ${e.message})`);
}
process.exit(result.status);

function localTimestamp() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
