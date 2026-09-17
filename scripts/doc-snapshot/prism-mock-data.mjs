export const DEFAULT_EXPLORER_URL = 'http://localhost/';

export function localDateText(now = new Date()) {
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function localDateTextDaysAgo(days, now = new Date()) {
  const date = new Date(now);
  date.setDate(date.getDate() - days);
  return localDateText(date);
}

export function normalizeBaseUrl(value) {
  return value.endsWith('/') ? value : `${value}/`;
}

export function urlFromBase(baseUrl, pathname) {
  return new URL(pathname.replace(/^\//, ''), baseUrl).toString();
}

export async function prepareMockData({ explorerUrl, date, startDate = date, endDate = date, resetData = true, skipData = false }) {
  if (skipData) {
    return;
  }
  await ensureExplorerReady(explorerUrl);
  if (resetData) {
    await clearMockData(explorerUrl);
  }
  await ensureMockData(explorerUrl, { startDate, endDate });
}

export async function ensureExplorerReady(explorerUrl) {
  const statusUrl = urlFromBase(explorerUrl, '/prism-explorer/api/mock/status');
  const response = await fetch(statusUrl);
  if (!response.ok) {
    throw new Error(`Prism Explorer is not ready: GET ${statusUrl} returned ${response.status}`);
  }
  const status = await response.json();
  if (!status.initDone) {
    throw new Error('Prism Explorer database initialization is not done yet.');
  }
}

export async function ensureMockData(explorerUrl, { startDate, endDate }) {
  const dataUrl = new URL(urlFromBase(explorerUrl, '/prism-explorer/api/mock/data'));
  dataUrl.searchParams.set('startDate', startDate);
  dataUrl.searchParams.set('endDate', endDate);

  return runMockTask(dataUrl, 'Mock data generation');
}

export async function clearMockData(explorerUrl) {
  const clearUrl = new URL(urlFromBase(explorerUrl, '/prism-explorer/api/mock/clear'));
  return runMockTask(clearUrl, 'Mock data cleanup');
}

async function runMockTask(taskUrl, label) {
  const response = await fetch(taskUrl, { method: 'POST' });
  if (!response.ok) {
    throw new Error(`${label} failed to start: ${response.status}`);
  }

  const started = await response.json();
  if (started.done) {
    if (started.error) {
      throw new Error(`${label} failed: ${started.message || started.error}`);
    }
    return started;
  }
  if (!started.taskId) {
    throw new Error(`${label} did not return taskId: ${JSON.stringify(started)}`);
  }

  const progressUrl = new URL('progress', taskUrl);
  progressUrl.searchParams.set('taskId', started.taskId);

  const deadline = Date.now() + 180_000;
  while (Date.now() < deadline) {
    await wait(1000);
    const progressResponse = await fetch(progressUrl);
    if (!progressResponse.ok) {
      throw new Error(`${label} progress check failed: ${progressResponse.status}`);
    }
    const progress = await progressResponse.json();
    if (progress.done) {
      if (progress.error) {
        throw new Error(`${label} failed: ${progress.message || progress.error}`);
      }
      return progress;
    }
  }

  throw new Error(`Timed out waiting for ${label}.`);
}

async function wait(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}
