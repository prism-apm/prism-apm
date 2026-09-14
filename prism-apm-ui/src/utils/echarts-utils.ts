import * as echarts from "echarts";
import i18n from '@/locale';

/**
 * Get ECharts locale based on current i18n locale
 * ECharts supports: 'EN', 'ZH'
 */
function getEchartsLocale(): 'EN' | 'ZH' {
    const locale = i18n.locale;
    if (locale && locale.toLowerCase().startsWith('zh')) {
        return 'ZH';
    }
    return 'EN';
}

const noOp = () => {
};

// Define a dummy proxy (Mock object)
// When any property or method is accessed, it returns an empty function
const dummyChart: any = new Proxy({}, {
    get: function () {
        // If for debugging, you can add a console.warn here to indicate which method was called but didn't take effect
        // console.warn(`[SafeEcharts] Called ${String(prop)}, but the DOM element does not exist, the operation has been ignored.`);

        // Return an empty function regardless of what method is called (such as showLoading, setOption)
        return noOp;
    }
});

export function showLoading(elementIdOrIds: string | string[]): void {
    if (Array.isArray(elementIdOrIds)) {
        elementIdOrIds.forEach(id => showLoading(id));
        return;
    }
    let chart = getEchartsInstanceByElementId(elementIdOrIds);
    chart.showLoading();
}

export function hideLoading(elementIdOrIds: string | string[]): void {
    if (Array.isArray(elementIdOrIds)) {
        elementIdOrIds.forEach(id => hideLoading(id));
        return;
    }
    let chart = getEchartsInstanceByElementId(elementIdOrIds);
    chart.hideLoading();
}

export function setOptionWithResize(elementId: string, option: any, notMerge = false): void {
    let chart = getEchartsInstanceByElementId(elementId);
    chart.clear();
    chart.resize();
    if (option.series.length <= 0 || option.series.map((a: any) => a.data).filter((a: any) => a.length > 0).length === 0) {
        option.graphic = getDefaultGraphicConfig();
    }
    chart.setOption(option, notMerge);
}

export function makeChartDisplayNodata(elementId: string | string[]): void {
    if (Array.isArray(elementId)) {
        elementId.forEach(id => makeChartDisplayNodata(id));
        return;
    }

    let chart = getEchartsInstanceByElementId(elementId);

    // 1. If it's a Mock proxy object, skip it directly
    if (chart === dummyChart) {
        return;
    }

    // 2. Try to get Option, if not, create a new one
    let option: any = chart.getOption();
    if (!option) {
        option = {
            grid: DEFAULT_GRID_CONFIG,
            xAxis: { show: false },
            yAxis: { show: false },
            tooltip: { show: false }
        };
    }

    // 3. Clear the canvas
    chart.clear();
    chart.resize();

    // 4. Force clear series
    option.series = [];

    // 5. Set no data style
    option.graphic = getDefaultGraphicConfig();

    // Disable tooltip to prevent mouse hover errors
    if (!option.tooltip) option.tooltip = {};
    option.tooltip.show = false;

    // 6. Apply configuration (notMerge = true)
    chart.setOption(option, true);
}

// Echarts config
export const DEFAULT_GRID_CONFIG = {
    left: '4%',
    right: '2%',
    top: '10%',
    bottom: '12%'
}

export const DEFAULT_TOOLBOX_CONFIG = {
    show: true,
    feature: {
        mark: { show: true },
        dataView: { show: true, readOnly: false },
        magicType: { show: true, type: ['line', 'bar'] },
        saveAsImage: { show: true }
    }
}

export function getDefaultGraphicConfig(): any[] {
    return [
        {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
                fill: '#999',
                text: (i18n as any).t('common.noData'),
                font: 'bold 20px Microsoft YaHei'
            }
        },
    ];
}

export function yAxisConfig(formatter: any): any {
    return {
        type: 'value',
        splitLine: { show: true, lineStyle: { color: '#00000019' } },
        lineStyle: {
            color: '#000',
            width: 1
        },
        axisLine: {
            lineStyle: {
                color: '#00000019',
                width: 1
            }
        },
        axisLabel: {
            color: '#666',
            formatter: formatter
        }
    };
}

export function xAxisConfig(data: any[]): any {
    return {
        type: 'category',
        boundaryGap: false,
        axisLine: {
            lineStyle: {
                color: '#00000019',
                width: 1
            }
        },
        axisLabel: {
            color: '#666'
        },
        data: data
    }
}

function getEchartsInstanceByElementId(elementId: string): any {
    let elementById = document.getElementById(elementId);
    if (elementById === null) {
        console.warn(`Cannot find element with ID ${elementId}, returning safe proxy object to prevent errors.`);
        return dummyChart;
    }
    let chart = echarts.getInstanceByDom(elementById);
    if (chart === undefined || chart === null) {
        chart = echarts.init(elementById, undefined, { locale: getEchartsLocale() });
    }
    return chart;
}
