import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
  DEFAULT_OUTPUT_DIR,
  EXPECTED_SCREENSHOTS,
  parseArgs,
  validateImageMetadata,
} from './capture-quickstart-screenshots.mjs';

test('default output directory is under the script directory', () => {
  assert.equal(DEFAULT_OUTPUT_DIR, new URL('./quickstart-shots', import.meta.url).pathname);
});

test('default mock data range ends today and starts six days ago', () => {
  const options = parseArgs([], { now: new Date(2026, 7, 13, 9, 30, 0) });

  assert.equal(options.date, '2026-08-13');
  assert.equal(options.startDate, '2026-08-07');
  assert.equal(options.endDate, '2026-08-13');
});

test('--date keeps the single-day mock data shortcut', () => {
  const options = parseArgs(['--date', '2026-08-13']);

  assert.equal(options.date, '2026-08-13');
  assert.equal(options.startDate, '2026-08-13');
  assert.equal(options.endDate, '2026-08-13');
});

test('custom mock data date range is accepted', () => {
  const options = parseArgs(['--start-date', '2026-08-07', '--end-date', '2026-08-13']);

  assert.equal(options.startDate, '2026-08-07');
  assert.equal(options.endDate, '2026-08-13');
});

test('custom mock data date range rejects an inverted range', () => {
  assert.throws(
    () => parseArgs(['--start-date', '2026-08-13', '--end-date', '2026-08-07']),
    /Invalid mock data date range/,
  );
});

test('mock data is reset before generation unless data work is skipped', () => {
  assert.equal(parseArgs([]).resetData, true);

  const skipDataOptions = parseArgs(['--skip-data']);
  assert.equal(skipDataOptions.skipData, true);
  assert.equal(skipDataOptions.resetData, false);
});

test('Prism Explorer screenshot language defaults to Chinese and supports English', () => {
  assert.equal(parseArgs([]).explorerLang, 'zh');
  assert.equal(parseArgs(['--explorer-lang', 'en']).explorerLang, 'en');
});

test('Prism APM UI screenshot language defaults to Chinese and supports English', () => {
  assert.equal(parseArgs([]).uiLang, 'zh-CN');
  assert.equal(parseArgs(['--ui-lang', 'en-US']).uiLang, 'en-US');
});

test('Prism Explorer screenshot language rejects unsupported values', () => {
  assert.throws(
    () => parseArgs(['--explorer-lang', 'fr']),
    /Invalid --explorer-lang value: fr/,
  );
});

test('Prism APM UI screenshot language rejects unsupported values', () => {
  assert.throws(
    () => parseArgs(['--ui-lang', 'fr-FR']),
    /Invalid --ui-lang value: fr-FR/,
  );
});

test('quickstart screenshot spec uses stable filenames and dimensions', () => {
  assert.deepEqual(
    EXPECTED_SCREENSHOTS.map((item) => item.file),
    [
      'prism-explorer-home.png',
      'prism-explorer-data-mock.png',
      'service-overview.png',
      'topology.png',
      'endpoint-detail.png',
    ],
  );

  for (const item of EXPECTED_SCREENSHOTS) {
    assert.equal(item.format, 'png');
    assert.equal(item.deviceScaleFactor, 2);
    assert.ok(item.viewport.width >= 1280);
    assert.ok(item.viewport.height >= 720);
    assert.ok(item.minBytes >= 10 * 1024);
    assert.equal(Object.hasOwn(item, 'mustContain'), false);
  }
});

test('UI screenshots require an explicit login before capture', () => {
  assert.deepEqual(
    EXPECTED_SCREENSHOTS.filter((item) => item.requiresLogin).map((item) => item.id),
    ['service-overview', 'topology', 'endpoint-detail'],
  );
});

test('screenshots affected by loading fades wait before capture', () => {
  assert.deepEqual(
    EXPECTED_SCREENSHOTS.filter((item) => item.settleAfterMs).map((item) => [item.id, item.settleAfterMs]),
    [
      ['service-overview', 3000],
      ['endpoint-detail', 3000],
    ],
  );
});

test('validateImageMetadata rejects missing or undersized images', () => {
  assert.deepEqual(
    validateImageMetadata({
      exists: false,
      file: 'missing.png',
      size: 0,
      width: 0,
      height: 0,
      spec: EXPECTED_SCREENSHOTS[0],
    }),
    ['missing.png does not exist'],
  );

  assert.deepEqual(
    validateImageMetadata({
      exists: true,
      file: 'small.png',
      size: 100,
      width: EXPECTED_SCREENSHOTS[0].viewport.width * EXPECTED_SCREENSHOTS[0].deviceScaleFactor,
      height: EXPECTED_SCREENSHOTS[0].viewport.height * EXPECTED_SCREENSHOTS[0].deviceScaleFactor,
      spec: EXPECTED_SCREENSHOTS[0],
    }),
    ['small.png is too small: 100 bytes'],
  );
});
