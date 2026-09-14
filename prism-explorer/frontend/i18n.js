(function () {
  const LANG_KEY = 'prism-explorer-lang';

  const dict = {
    zh: {
      // ── Common ──
      starting: '启动中...',
      pollFailed: '轮询失败: ',
      requestFailed: '请求失败: ',
      dateRangeSep: '至',
      genBtn: '开始生成',
      progressStarting: '准备开始...',

      // ── prism-explorer.html ──
      pageTitle: 'Prism Explorer',
      subtitleCards: '选择一个入口开始体验',
      subtitleWizard: '只需两步，开始体验 APM',
      cardDataMock: 'Data Mock',
      cardDataMockDesc: '重新生成 Mock 数据、清空数据',
      cardApmMain: 'APM 主界面',
      cardApmMainDesc: '查看链路追踪、拓扑图、监控大盘',
      step1Title: '生成演示数据',
      step1Desc: '选择日期范围，点击下方按钮自动生成演示数据。完成后即可进入 APM 主界面浏览。',
      step2Title: '数据已就绪',
      step2Desc: '演示数据已成功生成！点击下方按钮开始探索 APM 的功能吧。',
      enterApm: '👉 进入 APM 主界面',
      orDivider: '或者',
      advancedOptions: '高级选项：重新生成或清空数据 →',

      // ── prism-explorer-data.html ──
      dataPageTitle: 'Data Mock',
      dataSubtitle: '生成演示数据或清空已有数据',
      clearTitle: '清空数据',
      clearBtn: '清空数据',
      genTitle: '生成 Mock 数据',
      clearConfirm: '确定要清空所有数据吗？此操作不可撤销。',
      initWaiting: '等待初始化完成…',
    },

    en: {
      // ── Common ──
      starting: 'Starting...',
      pollFailed: 'Poll failed: ',
      requestFailed: 'Request failed: ',
      dateRangeSep: 'to',
      genBtn: 'Generate',
      progressStarting: 'Preparing...',

      // ── prism-explorer.html ──
      pageTitle: 'Prism Explorer',
      subtitleCards: 'Choose an entry to get started',
      subtitleWizard: 'Two steps to explore APM',
      cardDataMock: 'Data Mock',
      cardDataMockDesc: 'Regenerate mock data or clear data',
      cardApmMain: 'APM Dashboard',
      cardApmMainDesc: 'View trace, topology, and monitoring dashboards',
      step1Title: 'Generate demo data',
      step1Desc: 'Select a date range and click the button below to auto-generate demo data. Once complete, you can explore the APM dashboard.',
      step2Title: 'Data is ready',
      step2Desc: 'Demo data has been generated successfully! Click the button below to start exploring APM features.',
      enterApm: '👉 Open APM Dashboard',
      orDivider: 'or',
      advancedOptions: 'Advanced: regenerate or clear data →',

      // ── prism-explorer-data.html ──
      dataPageTitle: 'Data Mock',
      dataSubtitle: 'Generate demo data or clear existing data',
      clearTitle: 'Clear Data',
      clearBtn: 'Clear Data',
      genTitle: 'Generate Mock Data',
      clearConfirm: 'Are you sure you want to clear all data? This action cannot be undone.',
      initWaiting: 'Waiting for initialization…',
    },
  };

  function currentLang() {
    var stored = localStorage.getItem(LANG_KEY);
    if (stored === 'en' || stored === 'zh') return stored;
    var nav = (navigator.language || '').toLowerCase();
    return nav.indexOf('zh') === 0 ? 'zh' : 'en';
  }

  function setLang(lang) {
    localStorage.setItem(LANG_KEY, lang);
    apply(lang);
    updateToggleBtn(lang);
  }

  // ── Fixed Chinese messages returned by the backend -> English (exact match) ──
  var backendMsgs = {
    '请先执行「初始化数据库」再生成 Mock 数据': 'Please run "Initialize Database" first before generating mock data',
    '准备开始...': 'Preparing...',
    '开始清理...': 'Starting cleanup...',
    '任务不存在或已过期': 'Task does not exist or has expired',
  };

  // ── Pattern replacements for dynamic backend messages (strict long-to-short order to avoid substring collisions) ──
  var backendPatterns = [
    // Final executeData result
    ['✅ 完成！共处理 ', '✅ Complete! Processed '],
    [' 天数据\n', ' days of data\n'],
    ['日期范围：', 'Date range: '],
    ['后置脚本已执行\n', 'Post scripts executed\n'],
    // Final executeClear result
    ['✅ 清理完成！共执行 ', '✅ Cleanup complete! Executed '],
    // Error message prefixes
    ['❌ 执行失败：', '❌ Execution failed: '],
    ['❌ 清理失败：', '❌ Cleanup failed: '],
    // executeData progress (long patterns containing post-script or script terms must come before bare stage names)
    ['跳过（无可用脚本）', 'Skipped (no scripts available)'],
    [' 个，后置脚本 ', ', post scripts: '],
    ['样本脚本 ', 'Sample scripts: '],
    ['执行后置脚本 ', 'Post script: '],
    ['日期 ', 'Date: '],
    [' 选用 ', ' → '],
    // executeClear progress
    [' 个脚本待执行', ' scripts to execute'],
    ['正在执行 (', 'Running ('],
    ['共 ', 'Total: '],
    // Stage names (must come after all longer patterns containing these terms)
    ['数据生成', 'Data generation'],
    ['清理数据', 'Data cleanup'],
    ['后置脚本', 'Post scripts'],
    // Error message suffixes
    [' 阶段出错', ' stage error'],
    // Common suffixes
    [' 个脚本', ' scripts'],
    ['执行出错', 'Execution error'],
    ['清理出错', 'Cleanup error'],
    [' 个', ''],
  ];

  // ── Global translation function for dynamic JS text ──
  window.__ = function (key) {
    return dict[currentLang()][key] || key;
  };

  // ── Translate backend message text (Chinese -> current language) ──
  window.__t = function (text) {
    if (!text || currentLang() === 'zh') return text;
    // Try exact matches first.
    if (backendMsgs[text]) return backendMsgs[text];
    // Then apply pattern replacements.
    for (var i = 0; i < backendPatterns.length; i++) {
      text = text.split(backendPatterns[i][0]).join(backendPatterns[i][1]);
    }
    return text;
  };

  function apply(lang) {
    const t = dict[lang];

    // Traverse all elements with data-i18n.
    document.querySelectorAll('[data-i18n]').forEach(function (el) {
      const key = el.getAttribute('data-i18n');
      if (t[key] !== undefined && el.children.length === 0) {
        el.textContent = t[key];
      }
    });

    // Traverse inputs with data-i18n-placeholder.
    document.querySelectorAll('[data-i18n-placeholder]').forEach(function (el) {
      const key = el.getAttribute('data-i18n-placeholder');
      if (t[key] !== undefined) {
        el.placeholder = t[key];
      }
    });

    // Elements with data-i18n-title.
    document.querySelectorAll('[data-i18n-title]').forEach(function (el) {
      const key = el.getAttribute('data-i18n-title');
      if (t[key] !== undefined) {
        el.title = t[key];
      }
    });
  }

  window.__applyI18n = function () {
    apply(currentLang());
  };

  function updateToggleBtn(lang) {
    var btn = document.getElementById('langToggle');
    if (btn) {
      btn.textContent = lang === 'zh' ? 'EN' : '中';
      btn.title = lang === 'zh' ? 'Switch to English' : '切换到中文';
    }
  }

  function createToggleBtn() {
    var btn = document.createElement('button');
    btn.id = 'langToggle';
    btn.style.cssText =
      'position:fixed;top:16px;right:16px;z-index:9999;' +
      'width:40px;height:40px;padding:0;text-align:center;line-height:40px;' +
      'border-radius:50%;border:1px solid rgba(255,255,255,0.25);' +
      'background:rgba(255,255,255,0.08);color:#e2e8f0;font-size:13px;font-weight:600;' +
      'cursor:pointer;transition:all 0.2s;backdrop-filter:blur(4px);';
    btn.addEventListener('mouseenter', function () {
      btn.style.background = 'rgba(255,255,255,0.18)';
    });
    btn.addEventListener('mouseleave', function () {
      btn.style.background = 'rgba(255,255,255,0.08)';
    });
    btn.addEventListener('click', function () {
      var next = currentLang() === 'zh' ? 'en' : 'zh';
      setLang(next);
    });
    document.body.appendChild(btn);
  }

  // ── Page initialization ──
  document.addEventListener('DOMContentLoaded', function () {
    createToggleBtn();
    var lang = currentLang();
    apply(lang);
    updateToggleBtn(lang);
  });
})();
