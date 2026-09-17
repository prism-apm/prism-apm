# Documentation Screenshot Scripts

This directory contains the documentation tooling for generating and validating
screenshots used by the Prism APM documentation.

## Files

- `prism-mock-data.mjs`: shared Prism Explorer mock data helper. It checks Prism Explorer readiness, clears existing mock data, generates mock data for a date range, and polls task progress. When `--skip-data` is used, it skips all mock data work.
- `capture-quickstart-screenshots.mjs`: opens Prism Explorer and Prism APM UI with Playwright, prepares demo data through `prism-mock-data.mjs`, captures the quickstart screenshots, and validates the generated PNG files.
- `capture-service-metrics-screenshots.mjs`: opens Prism APM UI with Playwright, prepares demo data through `prism-mock-data.mjs`, captures the service metrics screenshots, and writes them as high-resolution PNG files.
- `capture-topology-screenshots.mjs`: opens Prism APM UI with Playwright, prepares demo data through `prism-mock-data.mjs`, captures the topology overview screenshot, and writes it as a high-resolution PNG file.
- `capture-quickstart-screenshots.test.mjs`: tests the screenshot script configuration, including output paths, default date handling, login requirements, image dimensions, and validation behavior.

## Prerequisites

Start the Prism Explorer Docker Compose environment before running the capture script.

By default, the script expects:

| Service | URL |
| --- | --- |
| Prism Explorer | `http://localhost/` |
| Prism APM UI | `http://localhost:8066/` |

Install Playwright for documentation tooling if it is not already available:

```bash
mkdir -p .doc-tools
npm install --prefix .doc-tools playwright
npx --prefix .doc-tools playwright install chromium
```

## Mock Data Preparation

Both capture scripts use `prism-mock-data.mjs` by default before opening the
browser.

The shared mock data flow does the following unless `--skip-data` is used:

1. Calls `GET /prism-explorer/api/mock/status` and requires `initDone: true`.
2. Calls `POST /prism-explorer/api/mock/clear`.
3. Calls `POST /prism-explorer/api/mock/data?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`.
4. Polls `progress?taskId=...` every second until the task completes or the 180 second timeout is reached.

By default, mock data is generated for the latest seven local dates: the end
date is today, and the start date is six days before today.

Use `--start-date YYYY-MM-DD` and `--end-date YYYY-MM-DD` to prepare screenshots
for a custom date range. Use `--date YYYY-MM-DD` as a single-day shortcut; it
sets both `startDate` and `endDate` to the same date.

Use `--skip-data` when the database already contains the desired mock data and
you only want to recapture screenshots. With `--skip-data`, the scripts do not
check Prism Explorer readiness, clear existing mock data, generate mock data, or
poll task progress.

## Capture Quickstart Screenshots

Run from the repository root:

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs
```

The default run does the following:

1. Prepares mock data through `prism-mock-data.mjs`.
2. Opens Prism Explorer and Prism APM UI with Chromium.
3. Logs in to Prism APM UI with `admin / apm-admin` before UI screenshots.
4. Captures the five quickstart screenshots.
5. Validates PNG format, dimensions, and minimum file size.

Default output goes to:

```text
scripts/doc-snapshot/quickstart-shots/
```

The default output directory is ignored by Git and is intended for local review.

Prism Explorer screenshots default to Chinese. To capture the Prism Explorer
pages and Prism APM UI pages in English instead, use:

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs --explorer-lang en --ui-lang en-US
```

The language settings are written to the Playwright browser context before the
pages load:

```js
localStorage.setItem('prism-explorer-lang', lang)
localStorage.setItem('locale', lang)
```

This makes the output independent from your regular browser language and
Playwright's default `navigator.language`.

## Capture Service Metrics Screenshots

Run from the repository root:

```bash
node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs
```

The default run does the following:

1. Prepares mock data through `prism-mock-data.mjs`.
2. Opens Prism APM UI with Chromium.
3. Logs in to Prism APM UI with `admin / apm-admin`.
4. Sets the time picker to `Today`.
5. Captures the nine service metrics screenshots.

The service metrics script always captures the APM UI with the time picker set
to `Today`. If you run it with `--skip-data`, Today endpoint data must already
exist in the database. At startup, the script prints this reminder to the
terminal:

```text
[service-metrics] --skip-data is enabled. The script will not generate mock data, so Today endpoint data must already exist.
```

If the script reaches the service endpoint overview and finds no Today endpoint
data, it prints this message to the terminal before failing with state details:

```text
[service-metrics] No endpoint data is available for Today. Run without --skip-data to generate today mock data, or generate today data before using --skip-data.
```

Default output goes to:

```text
scripts/doc-snapshot/service-metrics-shots/
```

The script captures:

- the service overview entry page
- the endpoint metrics overview for `auth-service`
- the internal details tab for `auth-service`
- the lower internal details view with both aggregation controls set to request category
- the lower internal details view with both aggregation controls set to request target
- the RED comparison tab for `auth-service`
- the RED comparison tab comparing two concrete hosts for `auth-service`
- the RED comparison tab comparing today with yesterday for `auth-service`
- the source statistics tab for `user-service` and endpoint `/demo/interface/path/697`

To overwrite the screenshots referenced by the service metrics documentation:

```bash
node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs --output docs/assets/zh-CN/features/service-metrics
```

To capture the APM UI in English, use:

```bash
node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs --ui-lang en-US
```

To overwrite the English service metrics documentation screenshots:

```bash
node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs --ui-lang en-US --output docs/assets/en/features/service-metrics
```

## Capture Topology Screenshots

Run from the repository root:

```bash
node scripts/doc-snapshot/capture-topology-screenshots.mjs
```

The default run does the following:

1. Prepares mock data through `prism-mock-data.mjs`.
2. Opens Prism APM UI with Chromium.
3. Logs in to Prism APM UI with `admin / apm-admin`.
4. Sets the time picker to `Today`.
5. Captures the topology overview screenshot.

The topology script defaults to Chinese. It writes `localStorage.locale` before
the page loads so output is independent from your regular browser state. To
capture the APM UI in English, use:

```bash
node scripts/doc-snapshot/capture-topology-screenshots.mjs --ui-lang en-US
```

Default output goes to:

```text
scripts/doc-snapshot/topology-shots/
```

The script captures `topology-overview.png`.

To overwrite screenshots for topology documentation assets:

```bash
node scripts/doc-snapshot/capture-topology-screenshots.mjs --output docs/assets/zh-CN/features/topology
```

To overwrite the English topology documentation screenshot:

```bash
node scripts/doc-snapshot/capture-topology-screenshots.mjs --ui-lang en-US --output docs/assets/en/features/topology
```

## Generated Files

Screenshots are captured at `deviceScaleFactor: 2`, so the PNG pixel size is
twice the browser viewport size.

### Quickstart

| File | Browser viewport | PNG size |
| --- | --- | --- |
| `prism-explorer-home.png` | `1280x720` | `2560x1440` |
| `prism-explorer-data-mock.png` | `1280x720` | `2560x1440` |
| `service-overview.png` | `1530x817` | `3060x1634` |
| `topology.png` | `1530x770` | `3060x1540` |
| `endpoint-detail.png` | `1530x737` | `3060x1474` |

### Service Metrics

| File | Browser viewport | PNG size |
| --- | --- | --- |
| `service-entry.png` | `1530x980` | `3060x1960` |
| `endpoint-overview.png` | `1530x980` | `3060x1960` |
| `endpoint-internal-details.png` | `1530x980` | `3060x1960` |
| `endpoint-internal-details-lower-by-type.png` | `1530x980` | `3060x1960` |
| `endpoint-internal-details-lower-by-target.png` | `1530x980` | `3060x1960` |
| `endpoint-source-stats.png` | `1530x980` | `3060x1960` |
| `endpoint-red-comparison.png` | `1530x980` | `3060x1960` |
| `endpoint-red-comparison-hosts.png` | `1530x980` | `3060x1960` |
| `endpoint-red-comparison-time.png` | `1530x980` | `3060x1960` |

### Topology

| File | Browser viewport | PNG size |
| --- | --- | --- |
| `topology-overview.png` | `1530x980` | `3060x1960` |

## Update Documentation Images

To overwrite the screenshots referenced by the quickstart document:

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs --output docs/assets/zh-CN/quickstart
```

To overwrite the English quickstart screenshots:

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs --explorer-lang en --ui-lang en-US --output docs/assets/en/quickstart
```

If you have already generated fresh mock data and only want to recapture the
pages without clearing or generating data again:

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs --skip-data --output docs/assets/zh-CN/quickstart
```

## Validate Existing Screenshots

Validate the default local output:

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs --validate-only
```

Validate the documentation assets:

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs --validate-only --output docs/assets/zh-CN/quickstart
```

## Useful Options

```bash
node scripts/doc-snapshot/capture-quickstart-screenshots.mjs --help
node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs --help
node scripts/doc-snapshot/capture-topology-screenshots.mjs --help
```

Common options:

- `--date YYYY-MM-DD`: generate mock data for one specific date. In the quickstart script, this date is also used by the endpoint detail query.
- `--start-date YYYY-MM-DD`: mock data generation start date. The default is six days before the local current date.
- `--end-date YYYY-MM-DD`: mock data generation end date. The default is the local current date.
- `--explorer-url URL`: use a non-default Prism Explorer URL.
- `--explorer-lang zh|en`: set the Prism Explorer screenshot language. Supported by `capture-quickstart-screenshots.mjs`; the default is `zh`.
- `--ui-url URL`: use a non-default Prism APM UI URL.
- `--ui-lang zh-CN|en-US`: set the Prism APM UI screenshot language. Supported by `capture-quickstart-screenshots.mjs`, `capture-service-metrics-screenshots.mjs`, and `capture-topology-screenshots.mjs`; the default is `zh-CN`.
- `--output DIR`: write screenshots to a custom directory.
- `--skip-data`: do not check Prism Explorer readiness, clear data, generate data, or poll mock data progress; only capture screenshots from the current data state. For `capture-service-metrics-screenshots.mjs`, Today endpoint data must already exist.
- `--validate-only`: validate existing screenshots without opening a browser.
- `--browser-executable PATH`: use a specific Chromium/Chrome executable. Supported by `capture-service-metrics-screenshots.mjs` and `capture-topology-screenshots.mjs`.
- `--headed`: run Chromium in headed mode for visual debugging.

## Run Tests

```bash
node --test scripts/doc-snapshot/capture-quickstart-screenshots.test.mjs
```

The tests do not open a browser or require Docker. They verify script-level
configuration and validation logic.

## Troubleshooting

### `Prism Explorer is not ready`

The Docker Compose environment is not ready, or `http://localhost/` is not
serving Prism Explorer yet. Wait for the services to finish starting and run the
script again.

### `endpoint-detail.png did not reach the expected state`

The endpoint detail page did not show endpoint rows in time. A normal default
run should clear and regenerate mock data first. If you used `--skip-data`,
confirm that the current database contains data for the date being queried.

### Service Metrics Has No Today Data

`capture-service-metrics-screenshots.mjs` and
`capture-topology-screenshots.mjs` set the APM UI time picker to `Today`. If
you run the service metrics script with `--skip-data` and the terminal prints:

```text
[service-metrics] No endpoint data is available for Today. Run without --skip-data to generate today mock data, or generate today data before using --skip-data.
```

run the script without `--skip-data` first:

```bash
node scripts/doc-snapshot/capture-service-metrics-screenshots.mjs
```

This regenerates the default one-week mock dataset, ending with today.

### Topology Looks Empty

Run the script without `--skip-data` first. This resets the mock dataset and
refreshes the aggregate data used by the topology page.

### Temporary `_failed-*.png` Files

When a page does not reach the expected state, the script writes a diagnostic
`_failed-*.png` file into the output directory. These files are for debugging
only and should not be committed.
