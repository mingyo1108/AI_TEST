#!/usr/bin/env node
// SQL 렌더러: 매퍼 XML과 파라미터 JSON으로 MyBatis가 실제로 만드는 SQL을 출력한다 (DB 연결 없음).
//
//   node scripts/render-sql.mjs StockLotMapper.selectLotList cases/selectLotList_01.json
//   node scripts/render-sql.mjs StockLotMapper.selectLotList --cases cases/        폴더의 케이스를 한 번에
//   node scripts/render-sql.mjs StockLotMapper.selectLotList p.json --inline       값을 채운 실행용 SQL
//   node scripts/render-sql.mjs XxxMapper.selectXxx p.json --xml old/XxxMapper.xml 지정한 XML로 렌더링
//
// 내부에서 Gradle 태스크 renderSql 을 한 번 실행한다 (gradlew / gradlew.bat 자동 선택).
import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { ROOT, runGradle } from './lib/run.mjs';

const args = process.argv.slice(2);
if (args.length === 0 || args[0] === '--help') {
  console.error('사용법: node scripts/render-sql.mjs <Mapper.queryId> [파라미터.json ...] [--cases <폴더>] [--xml <매퍼 XML>] [--inline]');
  process.exit(2);
}

// 파일 경로는 저장소 루트 기준으로 바꿔 넘긴다 (Gradle은 루트에서 실행됨)
const request = [];
for (let i = 0; i < args.length; i++) {
  const arg = args[i];
  if (arg === '--cases' || arg === '--xml') {
    request.push(arg, path.resolve(process.cwd(), args[++i] ?? '.'));
  } else if (arg.endsWith('.json')) {
    request.push(path.resolve(process.cwd(), arg));
  } else {
    request.push(arg);
  }
}

const requestDir = path.join(ROOT, 'build', 'render-sql');
mkdirSync(requestDir, { recursive: true });
const requestFile = path.join(requestDir, 'request.txt');
writeFileSync(requestFile, request.join('\n') + '\n');

const result = runGradle(['-q', '--console=plain', 'renderSql', `-PrenderRequest=${requestFile}`]);
process.stdout.write(result.stdout);
// Gradle 자체 메시지는 실패 원인만 보여 준다
if (result.stderr) {
  const lines = result.stderr.split(/\r?\n/).filter((l) => l && !l.startsWith('*') && !l.includes('gradle.org')
    && !l.startsWith('BUILD') && !l.includes('--stacktrace') && !l.includes('--info') && !l.includes('--scan'));
  if (lines.length) process.stderr.write(lines.slice(0, 30).join('\n') + '\n');
}
const renderFailed = /^-- (렌더링 오류|파라미터 JSON 오류)/m.test(result.stdout);
process.exit(result.status !== 0 ? 2 : renderFailed ? 1 : 0);
