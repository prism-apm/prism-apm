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
const REPO_ROOT = path.resolve(__dirname, '..');

const DEFAULT_UI_URL = 'http://localhost:8066/';
const DEFAULT_UI_LANG = 'zh-CN';
const DEFAULT_DEVICE_SCALE_FACTOR = 2;
const DEFAULT_OUTPUT_DIR = path.join(__dirname, 'topology-shots');
const SUPPORTED_UI_LANGS = new Set(['zh-CN', 'en-US']);

const SCREENSHOTS = [
  {
    id: 'topology-overview',
    file: 'topology-overview.png',
    viewport: { width: 1530, height: 980 },
  },
];

function usage(now = new Date()) {
  return `Usage: node scripts/capture-topology-screenshots.mjs [options]

Options:
  --date YYYY-MM-DD             Generate a single mock data date.
  --start-date YYYY-MM-DD       Mock data start date. Default: ${localDateTextDaysAgo(6, now)}
  --end-date YYYY-MM-DD         Mock data end date. Default: today (${localDateText(now)})
  --explorer-url URL            Prism Explorer base URL. Default: ${DEFAULT_EXPLORER_URL}
  --ui-url URL                  Prism APM UI base URL. Default: ${DEFAULT_UI_URL}
  --ui-lang zh-CN|en-US         Prism APM UI screenshot language. Default: ${DEFAULT_UI_LANG}
  --output DIR                  Screenshot output directory. Default: scripts/topology-shots
  --browser-executable PATH     Chromium/Chrome executable path. Optional fallback for local Playwright installs.
  --skip-data                   Do not clear or generate Prism Explorer mock data. Existing Today topology data is required.
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

async function gotoTopology(page, uiUrl) {
  await page.goto(urlFromBase(uiUrl, '#/topology/list'), { waitUntil: 'domcontentloaded' });
  await waitForTopologyReady(page);
  await setToday(page);
  await resetTopologyView(page);
  await waitForScreenshotReady(page, 2000);
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
  await waitForTopologyReady(page);
  await waitForScreenshotReady(page, 2500);
}

async function waitForTopologyReady(page) {
  await waitNoLoading(page);
  await page.waitForFunction(
    () => {
      const component = findTopologyComponent();
      const graph = component?.graph;
      const mountNode = document.querySelector('#mountNode');
      const rect = mountNode?.getBoundingClientRect();
      return (
        location.hash.includes('/topology/list') &&
        Boolean(document.querySelector('.topology-wrapper')) &&
        rect &&
        rect.width > 300 &&
        rect.height > 300 &&
        Boolean(mountNode.querySelector('canvas,svg')) &&
        graph?.getNodes().some((node) => node.isVisible()) &&
        graph?.getEdges().some((edge) => edge.isVisible())
      );

      function findTopologyComponent() {
        const root = document.querySelector('.topology-wrapper')?.__vue__;
        const queue = root ? [root] : [];
        const visited = new Set();

        while (queue.length > 0) {
          const current = queue.shift();
          if (!current || visited.has(current)) {
            continue;
          }
          visited.add(current);
          if (current.graph) {
            return current;
          }
          queue.push(...(current.$children || []));
          if (current.$parent) {
            queue.push(current.$parent);
          }
        }

        return null;
      }
    },
    null,
    { timeout: 30_000 },
  );
}

async function resetTopologyView(page) {
  await page.evaluate(() => {
    const component = findTopologyComponent();
    const graph = component?.graph;
    if (!component || !graph) {
      throw new Error('Topology graph is not available.');
    }

    component.unlockEdges?.(graph.getEdges());
    graph.getNodes().forEach((node) => node.show());
    graph.getEdges().forEach((edge) => edge.show());
    component.loadFocusToOptions?.();
    graph.layout();
    graph.fitView(20, null, true);

    function findTopologyComponent() {
      const root = document.querySelector('.topology-wrapper')?.__vue__;
      const queue = root ? [root] : [];
      const visited = new Set();

      while (queue.length > 0) {
        const current = queue.shift();
        if (!current || visited.has(current)) {
          continue;
        }
        visited.add(current);
        if (current.graph) {
          return current;
        }
        queue.push(...(current.$children || []));
        if (current.$parent) {
          queue.push(current.$parent);
        }
      }

      return null;
    }
  });
}

async function topologyState(page) {
  return page.evaluate(() => {
    const component = findTopologyComponent();
    const graph = component?.graph;
    if (!graph) {
      return { nodes: 0, edges: 0, visibleNodes: 0, visibleEdges: 0 };
    }
    return {
      nodes: graph.getNodes().length,
      edges: graph.getEdges().length,
      visibleNodes: graph.getNodes().filter((node) => node.isVisible()).length,
      visibleEdges: graph.getEdges().filter((edge) => edge.isVisible()).length,
    };

    function findTopologyComponent() {
      const root = document.querySelector('.topology-wrapper')?.__vue__;
      const queue = root ? [root] : [];
      const visited = new Set();

      while (queue.length > 0) {
        const current = queue.shift();
        if (!current || visited.has(current)) {
          continue;
        }
        visited.add(current);
        if (current.graph) {
          return current;
        }
        queue.push(...(current.$children || []));
        if (current.$parent) {
          queue.push(current.$parent);
        }
      }

      return null;
    }
  });
}

async function waitForScreenshotReady(page, settleAfterMs = 0) {
  await waitNoLoading(page);
  if (settleAfterMs > 0) {
    await wait(settleAfterMs);
  }
}

async function screenshot(page, outputDir, file) {
  await wait(500);
  const outputPath = path.join(outputDir, file);
  await page.screenshot({ path: outputPath, fullPage: false, type: 'png' });
  console.log(`captured ${file}`);
}

async function captureStep(page, outputDir, spec, action) {
  try {
    if (action) {
      await action();
    }
    await screenshot(page, outputDir, spec.file);
  } catch (error) {
    const failurePath = path.join(outputDir, `_failed-${spec.file}`);
    await page.screenshot({ path: failurePath, fullPage: false, type: 'png' }).catch(() => {});
    const state = await topologyState(page).catch(() => ({}));
    throw new Error(`${spec.file} did not reach the expected state.
Failure screenshot: ${failurePath}
Topology state: ${JSON.stringify(state)}

${error.message}`);
  }
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
    console.log('[topology] --skip-data is enabled. The script will not generate mock data, so Today topology data must already exist.');
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
      await runStep('open topology page', () => gotoTopology(page, options.uiUrl));
      await runStep('capture topology-overview.png', () => captureStep(page, options.outputDir, byId['topology-overview']));

      const state = await topologyState(page);
      console.log(`topology nodes/edges: ${state.nodes}/${state.edges}`);
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
