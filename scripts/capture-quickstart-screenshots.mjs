#!/usr/bin/env node

import { createRequire } from 'node:module';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import {
  DEFAULT_EXPLORER_URL,
  localDateText,
  localDateTextDaysAgo,
  normalizeBaseUrl,
  prepareMockData,
  urlFromBase,
} from './prism-mock-data.mjs';

export { localDateText } from './prism-mock-data.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '..');

const DEFAULT_UI_URL = 'http://localhost:8066/';
const DEFAULT_UI_LANG = 'zh-CN';
const DEFAULT_DEVICE_SCALE_FACTOR = 2;
const DEFAULT_EXPLORER_LANG = 'zh';
const SUPPORTED_UI_LANGS = new Set(['zh-CN', 'en-US']);
export const DEFAULT_OUTPUT_DIR = path.join(__dirname, 'quickstart-shots');

export const EXPECTED_SCREENSHOTS = [
  {
    id: 'explorer-home',
    file: 'prism-explorer-home.png',
    format: 'png',
    viewport: { width: 1280, height: 720 },
    deviceScaleFactor: DEFAULT_DEVICE_SCALE_FACTOR,
    minBytes: 10 * 1024,
  },
  {
    id: 'explorer-data-mock',
    file: 'prism-explorer-data-mock.png',
    format: 'png',
    viewport: { width: 1280, height: 720 },
    deviceScaleFactor: DEFAULT_DEVICE_SCALE_FACTOR,
    minBytes: 10 * 1024,
  },
  {
    id: 'service-overview',
    file: 'service-overview.png',
    format: 'png',
    viewport: { width: 1530, height: 817 },
    deviceScaleFactor: DEFAULT_DEVICE_SCALE_FACTOR,
    minBytes: 50 * 1024,
    requiresLogin: true,
    settleAfterMs: 3000,
  },
  {
    id: 'topology',
    file: 'topology.png',
    format: 'png',
    viewport: { width: 1530, height: 770 },
    deviceScaleFactor: DEFAULT_DEVICE_SCALE_FACTOR,
    minBytes: 50 * 1024,
    requiresLogin: true,
  },
  {
    id: 'endpoint-detail',
    file: 'endpoint-detail.png',
    format: 'png',
    viewport: { width: 1530, height: 737 },
    deviceScaleFactor: DEFAULT_DEVICE_SCALE_FACTOR,
    minBytes: 50 * 1024,
    requiresLogin: true,
    settleAfterMs: 3000,
  },
];

function usage(now = new Date()) {
  return `Usage: node scripts/capture-quickstart-screenshots.mjs [options]

Options:
  --date YYYY-MM-DD             Generate and query a single mock data date.
  --start-date YYYY-MM-DD       Mock data start date. Default: ${localDateTextDaysAgo(6, now)}
  --end-date YYYY-MM-DD         Mock data end date. Default: today (${localDateText(now)})
  --explorer-url URL            Prism Explorer base URL. Default: ${DEFAULT_EXPLORER_URL}
  --explorer-lang zh|en         Prism Explorer screenshot language. Default: ${DEFAULT_EXPLORER_LANG}
  --ui-url URL                  Prism APM UI base URL. Default: ${DEFAULT_UI_URL}
  --ui-lang zh-CN|en-US         Prism APM UI screenshot language. Default: ${DEFAULT_UI_LANG}
  --output DIR                  Screenshot output directory. Default: scripts/quickstart-shots
  --skip-data                   Do not clear or generate Prism Explorer mock data.
  --validate-only               Validate existing screenshots without opening a browser.
  --headed                      Run Chromium in headed mode.
  --help                        Show this help.
`;
}

export function parseArgs(argv, { now = new Date() } = {}) {
  const today = localDateText(now);
  const options = {
    date: today,
    startDate: localDateTextDaysAgo(6, now),
    endDate: today,
    explorerUrl: DEFAULT_EXPLORER_URL,
    explorerLang: DEFAULT_EXPLORER_LANG,
    uiUrl: DEFAULT_UI_URL,
    uiLang: DEFAULT_UI_LANG,
    outputDir: DEFAULT_OUTPUT_DIR,
    skipData: false,
    resetData: true,
    validateOnly: false,
    headed: false,
    help: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--help') {
      options.help = true;
    } else if (arg === '--skip-data') {
      options.skipData = true;
      options.resetData = false;
    } else if (arg === '--validate-only') {
      options.validateOnly = true;
    } else if (arg === '--headed') {
      options.headed = true;
    } else if (arg === '--date') {
      const date = requireValue(argv, index, arg);
      options.date = date;
      options.startDate = date;
      options.endDate = date;
      index += 1;
    } else if (arg === '--start-date') {
      options.startDate = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--end-date') {
      options.endDate = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--explorer-url') {
      options.explorerUrl = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--explorer-lang') {
      options.explorerLang = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--ui-url') {
      options.uiUrl = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--ui-lang') {
      options.uiLang = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--output') {
      options.outputDir = path.resolve(requireValue(argv, index, arg));
      index += 1;
    } else {
      throw new Error(`Unknown option: ${arg}\n\n${usage(now)}`);
    }
  }

  if (!/^\d{4}-\d{2}-\d{2}$/.test(options.date)) {
    throw new Error(`Invalid --date value: ${options.date}. Expected YYYY-MM-DD.`);
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(options.startDate)) {
    throw new Error(`Invalid --start-date value: ${options.startDate}. Expected YYYY-MM-DD.`);
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(options.endDate)) {
    throw new Error(`Invalid --end-date value: ${options.endDate}. Expected YYYY-MM-DD.`);
  }
  if (options.startDate > options.endDate) {
    throw new Error(`Invalid mock data date range: --start-date ${options.startDate} is after --end-date ${options.endDate}.`);
  }
  if (!['zh', 'en'].includes(options.explorerLang)) {
    throw new Error(`Invalid --explorer-lang value: ${options.explorerLang}. Expected zh or en.`);
  }
  if (!SUPPORTED_UI_LANGS.has(options.uiLang)) {
    throw new Error(`Invalid --ui-lang value: ${options.uiLang}. Expected zh-CN or en-US.`);
  }

  options.explorerUrl = normalizeBaseUrl(options.explorerUrl);
  options.uiUrl = normalizeBaseUrl(options.uiUrl);

  return options;
}

function requireValue(argv, index, optionName) {
  const value = argv[index + 1];
  if (!value || value.startsWith('--')) {
    throw new Error(`${optionName} requires a value`);
  }
  return value;
}

function endpointUrl(uiUrl, date) {
  const startTime = `${date} 00:00:00`;
  const endTime = `${addDays(date, 1)} 00:00:00`;
  return `${urlFromBase(uiUrl, '#/overview/transaction')}?serviceName=user-service&startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`;
}

function addDays(dateText, days) {
  const date = new Date(`${dateText}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

async function loadPlaywright() {
  const requireRoots = [
    path.join(REPO_ROOT, 'package.json'),
    path.join(REPO_ROOT, '.doc-tools/package.json'),
    path.join(REPO_ROOT, 'prism-apm-ui/package.json'),
  ];

  for (const packageJsonPath of requireRoots) {
    try {
      const scopedRequire = createRequire(packageJsonPath);
      return scopedRequire('playwright');
    } catch {
      // Try the next dependency root.
    }
  }

  throw new Error(`Playwright is required to capture screenshots.

Install it for documentation tooling with:

  mkdir -p .doc-tools
  npm install --prefix .doc-tools playwright
  npx --prefix .doc-tools playwright install chromium
`);
}

async function loginIfNeeded(page, targetUrl, uiLang) {
  const username = page.locator('#username');
  const password = page.locator('#password');
  const loginButton = page.locator('button[type="submit"]');

  if ((await username.count()) === 0 || (await password.count()) === 0 || (await loginButton.count()) === 0) {
    return;
  }

  await page.evaluate((lang) => {
    localStorage.setItem('locale', lang);
  }, uiLang);
  await username.fill('admin');
  await password.fill('apm-admin');
  await loginButton.click();
  await page.waitForFunction(() => !location.hash.includes('/login'), null, { timeout: 30_000 });
  await page.goto(targetUrl, { waitUntil: 'domcontentloaded' });
}

async function loginToUi(page, uiUrl, uiLang) {
  const loginUrl = urlFromBase(uiUrl, '#/login');
  await page.goto(loginUrl, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('#username', { state: 'visible', timeout: 30_000 });
  await page.evaluate(() => {
    localStorage.removeItem('apm_token');
    localStorage.removeItem('redirectPath');
  });
  await page.evaluate((lang) => {
    localStorage.setItem('locale', lang);
  }, uiLang);

  await page.locator('#username').fill('admin');
  await page.locator('#password').fill('apm-admin');
  await page.locator('button[type="submit"]').click();
  await page.waitForFunction(() => Boolean(localStorage.getItem('apm_token')), null, { timeout: 30_000 });
  await page.waitForFunction(() => !location.hash.includes('/login'), null, { timeout: 30_000 });
}

async function capturePage({ browser, outputDir, spec, url, waitFor, uiUrl, warmupUrl, warmupWaitFor, explorerLang, uiLang }) {
  const context = await browser.newContext({ viewport: spec.viewport, deviceScaleFactor: spec.deviceScaleFactor });
  if (explorerLang) {
    await context.addInitScript((lang) => {
      localStorage.setItem('prism-explorer-lang', lang);
    }, explorerLang);
  }
  if (uiLang) {
    await context.addInitScript((lang) => {
      localStorage.setItem('locale', lang);
    }, uiLang);
  }
  const page = await context.newPage();
  try {
    const initialUrl = warmupUrl || url;
    if (spec.requiresLogin) {
      await loginToUi(page, uiUrl, uiLang);
    }
    await page.goto(initialUrl, { waitUntil: 'domcontentloaded' });
    if (!spec.requiresLogin) {
      await loginIfNeeded(page, initialUrl, uiLang);
    }
    if (warmupUrl) {
      if (warmupWaitFor) {
        await warmupWaitFor(page);
      }
      await page.goto(url, { waitUntil: 'domcontentloaded' });
    }
    try {
      await waitFor(page);
      await waitForScreenshotReady(page, spec);
    } catch (error) {
      const failurePath = path.join(outputDir, `_failed-${spec.file}`);
      await page.screenshot({ path: failurePath, fullPage: false, type: 'png' });
      const bodyText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '');
      throw new Error(`${spec.file} did not reach the expected state.
URL: ${page.url()}
Failure screenshot: ${failurePath}
Page text:
${bodyText.slice(0, 2000)}

${error.message}`);
    }
    const outputPath = path.join(outputDir, spec.file);
    await page.screenshot({ path: outputPath, fullPage: false, type: 'png' });
    console.log(`captured ${spec.file}`);
  } finally {
    await context.close();
  }
}

async function waitForScreenshotReady(page, spec) {
  await page.waitForFunction(
    () => [...document.querySelectorAll('.ivu-spin-fix,.global-loading,[class*=loading],[class*=overlay]')]
      .every((element) => {
        const rect = element.getBoundingClientRect();
        const style = getComputedStyle(element);
        return (
          rect.width === 0 ||
          rect.height === 0 ||
          style.display === 'none' ||
          style.visibility === 'hidden' ||
          Number(style.opacity) === 0
        );
      }),
    null,
    { timeout: 30_000 },
  );
  if (spec.settleAfterMs) {
    await wait(spec.settleAfterMs);
  }
}

async function waitForExplorerHome(page) {
  await page.waitForFunction(
    () => {
      const links = [...document.querySelectorAll('a[href]')].map((link) => link.getAttribute('href') || '');
      return (
        location.pathname === '/' &&
        document.querySelectorAll('a.card').length >= 2 &&
        links.some((href) => href.includes('prism-explorer-data.html'))
      );
    },
    null,
    { timeout: 30_000 },
  );
}

async function waitForExplorerDataMock(page) {
  await page.waitForFunction(
    () => (
      location.pathname.endsWith('/prism-explorer-data.html') &&
      Boolean(document.querySelector('#startDateInput')) &&
      Boolean(document.querySelector('#endDateInput')) &&
      Boolean(document.querySelector('#clearBtn')) &&
      Boolean(document.querySelector('#genBtn'))
    ),
    null,
    { timeout: 30_000 },
  );
}

async function waitForOverview(page) {
  await page.waitForFunction(
    () => {
      const cards = [...document.querySelectorAll('.service-card')];
      const cardTexts = cards.map((card) => card.textContent || '');
      return (
        location.hash.includes('/overview/application') &&
        cards.length >= 2 &&
        cardTexts.some((text) => text.includes('auth-service')) &&
        cardTexts.some((text) => text.includes('user-service'))
      );
    },
    null,
    { timeout: 30_000 },
  );
}

async function waitForTopology(page) {
  await page.waitForFunction(
    () => {
      const mountNode = document.querySelector('#mountNode');
      const rect = mountNode?.getBoundingClientRect();
      return (
        location.hash.includes('/topology/list') &&
        Boolean(document.querySelector('.topology-wrapper')) &&
        rect &&
        rect.width > 300 &&
        rect.height > 300 &&
        Boolean(mountNode.querySelector('canvas,svg'))
      );
    },
    null,
    { timeout: 30_000 },
  );
  await wait(3000);
}

async function waitForEndpoint(page) {
  await page.waitForFunction(
    () => (
      location.hash.includes('/overview/transaction') &&
      [...document.querySelectorAll('.transaction-list li, tbody tr')]
        .some((element) => /\/demo\/interface\/path\/\d+/.test(element.textContent || ''))
    ),
    null,
    { timeout: 30_000 },
  );
}

async function wait(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function readImageMetadata(filePath) {
  try {
    const buffer = await fs.readFile(filePath);
    const pngSignature = '89504e470d0a1a0a';
    if (buffer.subarray(0, 8).toString('hex') !== pngSignature) {
      return { exists: true, format: 'unknown', size: buffer.length, width: 0, height: 0 };
    }
    return {
      exists: true,
      format: 'png',
      size: buffer.length,
      width: buffer.readUInt32BE(16),
      height: buffer.readUInt32BE(20),
    };
  } catch (error) {
    if (error.code === 'ENOENT') {
      return { exists: false, format: 'missing', size: 0, width: 0, height: 0 };
    }
    throw error;
  }
}

export function validateImageMetadata({ exists, file, format = 'png', size, width, height, spec }) {
  const errors = [];
  const expectedWidth = spec.viewport.width * spec.deviceScaleFactor;
  const expectedHeight = spec.viewport.height * spec.deviceScaleFactor;
  if (!exists) {
    return [`${file} does not exist`];
  }
  if (format !== spec.format) {
    errors.push(`${file} is ${format}, expected ${spec.format}`);
  }
  if (width !== expectedWidth || height !== expectedHeight) {
    errors.push(`${file} is ${width}x${height}, expected ${expectedWidth}x${expectedHeight}`);
  }
  if (size < spec.minBytes) {
    errors.push(`${file} is too small: ${size} bytes`);
  }
  return errors;
}

async function validateOutput(outputDir) {
  const allErrors = [];
  for (const spec of EXPECTED_SCREENSHOTS) {
    const metadata = await readImageMetadata(path.join(outputDir, spec.file));
    allErrors.push(...validateImageMetadata({ file: spec.file, spec, ...metadata }));
  }
  if (allErrors.length > 0) {
    throw new Error(`Screenshot validation failed:\n- ${allErrors.join('\n- ')}`);
  }
  console.log('validated quickstart screenshots');
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    console.log(usage());
    return;
  }

  await fs.mkdir(options.outputDir, { recursive: true });

  if (options.validateOnly) {
    await validateOutput(options.outputDir);
    return;
  }

  await prepareMockData(options);

  const { chromium } = await loadPlaywright();
  const browser = await chromium.launch({ headless: !options.headed });
  try {
    const byId = Object.fromEntries(EXPECTED_SCREENSHOTS.map((spec) => [spec.id, spec]));
    await capturePage({
      browser,
      outputDir: options.outputDir,
      spec: byId['explorer-home'],
      url: options.explorerUrl,
      explorerLang: options.explorerLang,
      waitFor: waitForExplorerHome,
    });
    await capturePage({
      browser,
      outputDir: options.outputDir,
      spec: byId['explorer-data-mock'],
      url: urlFromBase(options.explorerUrl, '/prism-explorer-data.html'),
      explorerLang: options.explorerLang,
      waitFor: waitForExplorerDataMock,
    });
    await capturePage({
      browser,
      outputDir: options.outputDir,
      spec: byId['service-overview'],
      url: urlFromBase(options.uiUrl, '#/overview/application'),
      uiUrl: options.uiUrl,
      explorerLang: options.explorerLang,
      uiLang: options.uiLang,
      waitFor: waitForOverview,
    });
    await capturePage({
      browser,
      outputDir: options.outputDir,
      spec: byId.topology,
      url: urlFromBase(options.uiUrl, '#/topology/list'),
      uiUrl: options.uiUrl,
      explorerLang: options.explorerLang,
      uiLang: options.uiLang,
      waitFor: waitForTopology,
    });
    await capturePage({
      browser,
      outputDir: options.outputDir,
      spec: byId['endpoint-detail'],
      url: endpointUrl(options.uiUrl, options.date),
      uiUrl: options.uiUrl,
      explorerLang: options.explorerLang,
      uiLang: options.uiLang,
      warmupUrl: urlFromBase(options.uiUrl, '#/overview/application'),
      warmupWaitFor: waitForOverview,
      waitFor: waitForEndpoint,
    });
  } finally {
    await browser.close();
  }

  await validateOutput(options.outputDir);
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
