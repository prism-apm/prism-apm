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

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');

const DEFAULT_UI_URL = 'http://localhost:8066/';
const DEFAULT_UI_LANG = 'zh-CN';
const DEFAULT_DEVICE_SCALE_FACTOR = 2;
const DEFAULT_OUTPUT_DIR = path.join(__dirname, 'service-metrics-shots');
const DEFAULT_MACHINE_ID = 'All Hosts';
const SUPPORTED_UI_LANGS = new Set(['zh-CN', 'en-US']);

const SCREENSHOTS = [
  {
    id: 'service-entry',
    file: 'service-entry.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-overview',
    file: 'endpoint-overview.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-internal-details',
    file: 'endpoint-internal-details.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-internal-details-lower-by-type',
    file: 'endpoint-internal-details-lower-by-type.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-internal-details-lower-by-target',
    file: 'endpoint-internal-details-lower-by-target.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-red-comparison',
    file: 'endpoint-red-comparison.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-red-comparison-hosts',
    file: 'endpoint-red-comparison-hosts.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-red-comparison-time',
    file: 'endpoint-red-comparison-time.png',
    viewport: { width: 1530, height: 980 },
  },
  {
    id: 'endpoint-source-stats',
    file: 'endpoint-source-stats.png',
    viewport: { width: 1530, height: 980 },
  },
];

const TAB_INDEXES = Object.freeze({
  INTERNAL_DETAILS: 1,
  SOURCE_STATS: 2,
  RED_COMPARISON: 3,
});

function usage(now = new Date()) {
  return `Usage: node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs [options]

Options:
  --date YYYY-MM-DD             Generate a single mock data date.
  --start-date YYYY-MM-DD       Mock data start date. Default: ${localDateTextDaysAgo(6, now)}
  --end-date YYYY-MM-DD         Mock data end date. Default: today (${localDateText(now)})
  --explorer-url URL            Prism Explorer base URL. Default: ${DEFAULT_EXPLORER_URL}
  --ui-url URL                  Prism APM UI base URL. Default: ${DEFAULT_UI_URL}
  --ui-lang zh-CN|en-US         Prism APM UI screenshot language. Default: ${DEFAULT_UI_LANG}
  --output DIR                  Screenshot output directory. Default: scripts/doc-snapshot/service-metrics-shots
  --browser-executable PATH     Chromium/Chrome executable path. Optional fallback for local Playwright installs.
  --skip-data                   Do not clear or generate Prism Explorer mock data. Existing Today data is required.
  --headed                      Run Chromium in headed mode.
  --help                        Show this help.
`;
}

function parseArgs(argv, { now = new Date() } = {}) {
  const today = localDateText(now);
  const options = {
    date: today,
    startDate: localDateTextDaysAgo(6, now),
    endDate: today,
    explorerUrl: DEFAULT_EXPLORER_URL,
    uiUrl: DEFAULT_UI_URL,
    uiLang: DEFAULT_UI_LANG,
    outputDir: DEFAULT_OUTPUT_DIR,
    browserExecutable: '',
    skipData: false,
    resetData: true,
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
    } else if (arg === '--ui-url') {
      options.uiUrl = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--ui-lang') {
      options.uiLang = requireValue(argv, index, arg);
      index += 1;
    } else if (arg === '--output') {
      options.outputDir = path.resolve(requireValue(argv, index, arg));
      index += 1;
    } else if (arg === '--browser-executable') {
      options.browserExecutable = path.resolve(requireValue(argv, index, arg));
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

async function findBundledChromiumExecutable() {
  const cacheRoot = path.join(process.env.HOME || '', 'Library/Caches/ms-playwright');
  if (!cacheRoot) {
    return '';
  }

  try {
    const entries = await fs.readdir(cacheRoot, { withFileTypes: true });
    const chromiumDirs = entries
      .filter((entry) => entry.isDirectory() && entry.name.startsWith('chromium_headless_shell-'))
      .map((entry) => entry.name)
      .sort()
      .reverse();

    for (const dir of chromiumDirs) {
      const executablePath = path.join(cacheRoot, dir, 'chrome-headless-shell-mac-arm64', 'chrome-headless-shell');
      try {
        await fs.access(executablePath);
        return executablePath;
      } catch {
        // Try the next cached browser.
      }
    }
  } catch {
    // No local Playwright browser cache is available.
  }

  return '';
}

async function launchBrowser(chromium, options) {
  const launchOptions = { headless: !options.headed };
  if (options.browserExecutable) {
    launchOptions.executablePath = options.browserExecutable;
  }

  try {
    return await chromium.launch(launchOptions);
  } catch (error) {
    if (options.browserExecutable) {
      throw error;
    }

    const fallbackExecutable = await findBundledChromiumExecutable();
    if (!fallbackExecutable) {
      throw error;
    }

    return chromium.launch({
      ...launchOptions,
      executablePath: fallbackExecutable,
    });
  }
}

async function loginToUi(page, uiUrl, uiLang) {
  await page.goto(urlFromBase(uiUrl, '#/login'), { waitUntil: 'domcontentloaded' });
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

async function waitNoLoading(page) {
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
}

async function setToday(page) {
  const trigger = page.locator('.date-trigger');
  await trigger.waitFor({ state: 'visible', timeout: 30_000 });

  if ((await trigger.innerText()).trim() === 'Today') {
    return;
  }

  await trigger.click();
  await page.getByText('Today', { exact: true }).click();
  await page.waitForFunction(() => document.body.innerText.includes('Today'), null, { timeout: 30_000 });
  await waitForScreenshotReady(page, 2500);
}

async function gotoOverview(page, uiUrl) {
  await page.goto(urlFromBase(uiUrl, '#/overview/application'), { waitUntil: 'domcontentloaded' });
  await waitForServiceOverviewReady(page);
  await waitForScreenshotReady(page, 1000);
}

async function openService(page, uiUrl, serviceName) {
  await gotoOverview(page, uiUrl);
  await setToday(page);

  const card = page.locator('.service-card').filter({ hasText: serviceName });
  const count = await card.count();
  if (count !== 1) {
    throw new Error(`Expected one ${serviceName} card, got ${count}`);
  }

  await card.click();
  await waitForEndpointOverviewReady(page);
  await waitForScreenshotReady(page, 3000);
}

async function clickTabAt(page, tabIndex, description) {
  const tabs = page.locator('.custom-tab-wrapper .ivu-tabs-tab');
  const count = await tabs.count();
  if (count <= tabIndex) {
    throw new Error(`Expected tab index ${tabIndex} for ${description}, got ${count} tabs`);
  }

  await tabs.nth(tabIndex).click();
  await page.waitForFunction(
    (index) => document.querySelectorAll('.custom-tab-wrapper .ivu-tabs-tab')[index]
      ?.classList.contains('ivu-tabs-tab-active'),
    tabIndex,
    { timeout: 30_000 },
  );
  await waitForScreenshotReady(page, 3000);
}

async function selectEndpoint(page, endpoint) {
  const item = page.locator('.transaction-list li').filter({ hasText: endpoint });
  const count = await item.count();
  if (count !== 1) {
    throw new Error(`Expected one endpoint ${endpoint}, got ${count}`);
  }

  await item.click();
  await waitForScreenshotReady(page, 3000);
}

async function endpointList(page) {
  return page.evaluate(() => {
    const seen = new Set();
    return [...document.querySelectorAll('.transaction-list li')]
      .map((element) => {
        const text = element.textContent.trim();
        const match = text.match(/\/demo\/interface\/path\/\d+/);
        return match ? match[0] : '';
      })
      .filter((endpoint) => endpoint && !seen.has(endpoint) && seen.add(endpoint));
  });
}

async function chooseEndpointWithData(page) {
  const endpoints = await endpointList(page);

  for (const endpoint of endpoints.slice(0, 12)) {
    await selectEndpoint(page, endpoint);
    const rows = await dataRowCount(page);

    if (rows > 0) {
      return endpoint;
    }
  }

  if (endpoints.length === 0) {
    throw new Error('No endpoint was available in the endpoint list.');
  }

  return endpoints[0];
}

async function setInternalDetailsGroupMode(page, mode) {
  await page.evaluate((targetMode) => {
    const optionIndex = targetMode === 'AN' ? 0 : 1;
    const groups = [...document.querySelectorAll('.ivu-radio-group')]
      .filter((group) => {
        const rect = group.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
      })
      .slice(0, 2);

    if (groups.length < 2) {
      throw new Error(`Expected two internal detail radio groups, got ${groups.length}`);
    }

    for (const group of groups) {
      const option = [...group.querySelectorAll('.ivu-radio-wrapper')][optionIndex];
      if (!option) {
        throw new Error(`Could not find radio option ${targetMode} at index ${optionIndex}`);
      }

      option.click();
    }
  }, mode);

  await page.waitForFunction(
    (targetMode) => {
      const optionIndex = targetMode === 'AN' ? 0 : 1;
      const groups = [...document.querySelectorAll('.ivu-radio-group')]
        .filter((group) => {
          const rect = group.getBoundingClientRect();
          return rect.width > 0 && rect.height > 0;
        })
        .slice(0, 2);

      return groups.length === 2 && groups.every((group) => [...group.querySelectorAll('.ivu-radio-wrapper')][optionIndex]
        ?.classList.contains('ivu-radio-wrapper-checked'));
    },
    mode,
    { timeout: 30_000 },
  );

  await waitForScreenshotReady(page, 3000);
}

async function scrollToInternalDetailsLowerPart(page) {
  await page.evaluate(() => {
    const chart = document.getElementById('iface-analyse-chart');
    const top = chart
      ? chart.getBoundingClientRect().top + window.scrollY - 140
      : document.documentElement.scrollHeight * 0.35;
    window.scrollTo(0, Math.max(0, top));
  });
  await wait(500);
}

async function waitForScreenshotReady(page, settleAfterMs = 0) {
  await waitNoLoading(page);
  if (settleAfterMs > 0) {
    await wait(settleAfterMs);
  }
}

async function screenshot(page, outputDir, file, { scrollToTop = true } = {}) {
  if (scrollToTop) {
    await page.evaluate(() => window.scrollTo(0, 0));
  }
  await wait(500);
  const outputPath = path.join(outputDir, file);
  await page.screenshot({ path: outputPath, fullPage: false, type: 'png' });
  console.log(`captured ${file}`);
}

async function capture(page, outputDir, spec, assertReady) {
  if (assertReady) {
    await assertReady(page);
  }
  await screenshot(page, outputDir, spec.file);
}

async function captureInternalDetailsLowerPart(page, outputDir, spec, mode) {
  await setInternalDetailsGroupMode(page, mode);
  await waitForInternalDetailsReady(page);
  await scrollToInternalDetailsLowerPart(page);
  await screenshot(page, outputDir, spec.file, { scrollToTop: false });
}

async function hostOptions(page) {
  await page.waitForFunction(
    () => [...document.querySelectorAll('.select-container .ivu-select-dropdown-list .ivu-select-item')]
      .filter((item) => {
        const text = item.textContent.trim();
        return text && text !== 'All Hosts';
      })
      .length >= 2,
    null,
    { timeout: 30_000 },
  );

  return page.evaluate(() => [...document.querySelectorAll('.select-container .ivu-select-dropdown-list .ivu-select-item')]
    .map((item) => item.textContent.trim())
    .filter((item) => item && item !== 'All Hosts'));
}

async function setCompareMode(page, mode) {
  const modeIndex = mode === 'HOST' ? 0 : 1;
  await page.evaluate((targetMode) => {
    const compareControl = document.querySelector('.compare-control-container')?.__vue__;
    if (!compareControl?.handleModeChange) {
      throw new Error('Compare control Vue instance is not available.');
    }
    compareControl.handleModeChange(targetMode);
  }, mode);

  await page.waitForFunction(
    (index) => document.querySelectorAll('.compare-control-container .ivu-radio-wrapper')[index]
      ?.classList.contains('ivu-radio-wrapper-checked'),
    modeIndex,
    { timeout: 30_000 },
  );
}

async function setHostComparison(page) {
  await setCompareMode(page, 'HOST');
  await page.waitForFunction(() => Boolean(document.querySelector('.select-container .multi-service-option')), null, { timeout: 30_000 });
  const hosts = (await hostOptions(page)).slice(0, 2);
  if (hosts.length < 2) {
    throw new Error('Host comparison needs at least two concrete hosts.');
  }

  await page.evaluate((selectedHosts) => {
    const machineSelect = document.querySelector('.select-container')?.__vue__;
    if (!machineSelect?.handleMultipleChange) {
      throw new Error('Machine select Vue instance does not support multiple host selection.');
    }
    machineSelect.handleMultipleChange(selectedHosts);
  }, hosts);

  await page.waitForFunction(
    (selectedHosts) => selectedHosts.every((host) => document.querySelector('.select-container')?.textContent.includes(host)),
    hosts,
    { timeout: 30_000 },
  );
  await waitForRedComparisonReady(page);
  await waitForScreenshotReady(page, 6000);
  return hosts;
}

async function setTimeComparison(page) {
  await setCompareMode(page, 'TIME');
  await page.waitForFunction(() => Boolean(document.querySelector('.select-container .service-option')), null, { timeout: 30_000 });

  await page.evaluate((defaultMachineId) => {
    const machineSelect = document.querySelector('.select-container')?.__vue__;
    if (!machineSelect?.handleSingleChange) {
      throw new Error('Machine select Vue instance does not support single host selection.');
    }
    machineSelect.internalValue = defaultMachineId;
    machineSelect.handleSingleChange(defaultMachineId);
  }, DEFAULT_MACHINE_ID);

  await page.waitForFunction(
    (defaultMachineId) => document.querySelector('.select-container')?.textContent.includes(defaultMachineId),
    DEFAULT_MACHINE_ID,
    { timeout: 30_000 },
  );

  const compareDate = `${localDateTextDaysAgo(1)} 00:00:00`;

  await page.evaluate((date) => {
    const compareControl = document.querySelector('.compare-control-container')?.__vue__;
    if (!compareControl) {
      throw new Error('Compare control Vue instance is not available.');
    }
    compareControl.internalDate = date;
    compareControl.$emit('update:date', date);
    compareControl.$nextTick(() => compareControl.handleDateConfirm());
  }, compareDate);

  await page.waitForFunction(
    (date) => document.querySelector('.compare-control-container .ivu-date-picker input')?.value.includes(date.slice(0, 10)),
    compareDate,
    { timeout: 30_000 },
  );
  await waitForRedComparisonReady(page);
  await waitForScreenshotReady(page, 6000);
  return { host: DEFAULT_MACHINE_ID, compareDate };
}

async function captureRedComparisonVariant(page, outputDir, spec, setup) {
  const details = await setup(page);
  await capture(page, outputDir, spec, waitForRedComparisonReady);
  return details;
}

async function waitForServiceOverviewReady(page) {
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

async function waitForEndpointListReady(page) {
  await page.waitForFunction(
    () => (
      location.hash.includes('/overview/transaction') &&
      [...document.querySelectorAll('.transaction-list li')]
        .some((element) => /\/demo\/interface\/path\/\d+/.test(element.textContent || ''))
    ),
    null,
    { timeout: 30_000 },
  );
}

async function waitForEndpointOverviewReady(page) {
  await page.waitForFunction(() => location.hash.includes('/overview/transaction'), null, { timeout: 30_000 });
  await waitNoLoading(page);

  try {
    await page.waitForFunction(
      () => [...document.querySelectorAll('tbody tr')]
        .some((row) => /\/demo\/interface\/path\/\d+/.test(row.textContent || '') && row.querySelectorAll('td').length > 1),
      null,
      { timeout: 30_000 },
    );
  } catch (error) {
    const state = await endpointOverviewState(page);
    if (state.date === 'Today' && state.endpointRows === 0) {
      console.log('[service-metrics] No endpoint data is available for Today. Run without --skip-data to generate today mock data, or generate today data before using --skip-data.');
    }
    throw new Error(`Endpoint overview did not show endpoint rows for the selected time range. If you used --skip-data, generate mock data for today first or run without --skip-data. State: ${JSON.stringify(state)}`);
  }
}

async function endpointOverviewState(page) {
  return page.evaluate(() => {
    const visibleRows = [...document.querySelectorAll('tbody tr')]
      .filter((row) => {
        const rect = row.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0 && row.querySelectorAll('td').length > 1;
      });
    return {
      hash: location.hash,
      date: document.querySelector('.date-trigger')?.textContent.trim() || '',
      visibleTableRows: visibleRows.length,
      endpointRows: visibleRows
        .map((row) => (row.textContent || '').trim())
        .filter((text) => /\/demo\/interface\/path\/\d+/.test(text))
        .length,
    };
  });
}

async function waitForInternalDetailsReady(page) {
  await waitForEndpointListReady(page);
  await waitForChartsReady(page, ['iface-RED-chart', 'iface-analyse-chart']);
  await waitForDataRows(page);
}

async function waitForRedComparisonReady(page) {
  await waitForEndpointListReady(page);
  await waitForChartsReady(page, ['iface-duration-chart', 'iface-count-chart', 'iface-error-chart']);
}

async function waitForSourceStatsReady(page) {
  await waitForEndpointListReady(page);
  await waitForChartsReady(page, [
    'newWhoCallMeIfacePieChart0',
    'newWhoCallMeIfacePieChart1',
    'newWhoCallMeIfacePieChart2',
  ]);
  await page.waitForFunction(
    () => document.body.textContent.includes('/demo/interface/path/697'),
    null,
    { timeout: 30_000 },
  );
  await waitForDataRows(page);
}

async function waitForChartsReady(page, chartIds) {
  await page.waitForFunction(
    (ids) => ids.every((id) => {
      const chart = document.getElementById(id);
      const rect = chart?.getBoundingClientRect();
      return (
        rect &&
        rect.width > 100 &&
        rect.height > 100 &&
        Boolean(chart.querySelector('canvas,svg'))
      );
    }),
    chartIds,
    { timeout: 30_000 },
  );
}

async function waitForDataRows(page) {
  await page.waitForFunction(
    () => [...document.querySelectorAll('tbody tr')]
      .filter((row) => {
        const rect = row.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0 && row.querySelectorAll('td').length > 1;
      })
      .length > 0,
    null,
    { timeout: 30_000 },
  );
}

async function dataRowCount(page) {
  return page.evaluate(() => [...document.querySelectorAll('tbody tr')]
    .filter((row) => {
      const rect = row.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0 && row.querySelectorAll('td').length > 1;
    })
    .length);
}

async function wait(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function runStep(name, action) {
  try {
    return await action();
  } catch (error) {
    error.message = `${name}: ${error.message}`;
    throw error;
  }
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    console.log(usage());
    return;
  }

  if (options.skipData) {
    console.log('[service-metrics] --skip-data is enabled. The script will not generate mock data, so Today endpoint data must already exist.');
  }

  await fs.mkdir(options.outputDir, { recursive: true });
  await prepareMockData(options);

  const { chromium } = await loadPlaywright();
  const browser = await launchBrowser(chromium, options);

  try {
    const context = await browser.newContext({
      viewport: SCREENSHOTS[0].viewport,
      deviceScaleFactor: DEFAULT_DEVICE_SCALE_FACTOR,
    });
    await context.addInitScript((lang) => {
      localStorage.setItem('locale', lang);
    }, options.uiLang);
    const page = await context.newPage();

    try {
      const byId = Object.fromEntries(SCREENSHOTS.map((spec) => [spec.id, spec]));

      await runStep('login to APM UI', () => loginToUi(page, options.uiUrl, options.uiLang));

      await runStep('open service overview', () => gotoOverview(page, options.uiUrl));
      await runStep('set time range to Today', () => setToday(page));
      await runStep('capture service-entry.png', () => capture(page, options.outputDir, byId['service-entry'], waitForServiceOverviewReady));

      await runStep('open auth-service endpoint overview', () => openService(page, options.uiUrl, 'auth-service'));
      await runStep('capture endpoint-overview.png', () => capture(page, options.outputDir, byId['endpoint-overview'], waitForEndpointOverviewReady));

      await runStep('open internal details tab', () => clickTabAt(page, TAB_INDEXES.INTERNAL_DETAILS, 'internal details'));
      const authEndpoint = await runStep('choose auth-service endpoint with data', () => chooseEndpointWithData(page));
      await runStep('capture endpoint-internal-details.png', () => capture(page, options.outputDir, byId['endpoint-internal-details'], waitForInternalDetailsReady));
      await runStep('capture endpoint-internal-details-lower-by-type.png', () => captureInternalDetailsLowerPart(
        page,
        options.outputDir,
        byId['endpoint-internal-details-lower-by-type'],
        'AN',
      ));
      await runStep('capture endpoint-internal-details-lower-by-target.png', () => captureInternalDetailsLowerPart(
        page,
        options.outputDir,
        byId['endpoint-internal-details-lower-by-target'],
        'TN',
      ));

      await runStep('open RED comparison tab', () => clickTabAt(page, TAB_INDEXES.RED_COMPARISON, 'RED comparison'));
      if (authEndpoint) {
        await runStep('select auth-service RED endpoint', () => selectEndpoint(page, authEndpoint));
      }
      await runStep('capture endpoint-red-comparison.png', () => capture(page, options.outputDir, byId['endpoint-red-comparison'], waitForRedComparisonReady));
      const hostCompareHosts = await runStep('capture endpoint-red-comparison-hosts.png', () => captureRedComparisonVariant(
        page,
        options.outputDir,
        byId['endpoint-red-comparison-hosts'],
        setHostComparison,
      ));
      const timeCompareDetails = await runStep('capture endpoint-red-comparison-time.png', () => captureRedComparisonVariant(
        page,
        options.outputDir,
        byId['endpoint-red-comparison-time'],
        setTimeComparison,
      ));

      await runStep('open user-service endpoint overview', () => openService(page, options.uiUrl, 'user-service'));
      await runStep('open source stats tab', () => clickTabAt(page, TAB_INDEXES.SOURCE_STATS, 'source stats'));
      await runStep('select /demo/interface/path/697 endpoint', () => selectEndpoint(page, '/demo/interface/path/697'));
      await runStep('capture endpoint-source-stats.png', () => capture(page, options.outputDir, byId['endpoint-source-stats'], waitForSourceStatsReady));

      console.log(`auth-service endpoint used for internal/RED: ${authEndpoint}`);
      console.log(`host comparison used hosts: ${hostCompareHosts.join(', ')}`);
      console.log(`time comparison used host/date: ${timeCompareDetails.host}, ${timeCompareDetails.compareDate}`);
    } finally {
      await context.close();
    }
  } finally {
    await browser.close();
  }
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
