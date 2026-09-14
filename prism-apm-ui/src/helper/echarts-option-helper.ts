import {DEFAULT_GRID_CONFIG, DEFAULT_TOOLBOX_CONFIG, xAxisConfig, yAxisConfig} from "@/utils/echarts-utils";
import {formatTinyNumber} from '@/utils/number-utils';
import * as echarts from "echarts";
import i18n from '@/locale';

const CHART_COLORS = {
    RESPONSE_TIME: '#32d2c9',
    CALL_COUNT: '#f8a10f',
    ERROR_COUNT: '#c100c1',
} as const;

export function getComparableEchartsOption(compareOptions: any, timeArr: any[], data: any, compareData?: any): any {
    let is_compare = compareOptions !== null && compareOptions !== undefined;
    let duration_option: any, count_option: any, error_option: any;

    if (!is_compare) {
        duration_option = getEchartsOption(timeArr, data.durations, (i18n as any).t('chart.responseTime'), CHART_COLORS.RESPONSE_TIME);
        count_option = getEchartsOption(timeArr, data.counts, (i18n as any).t('chart.callCount'), CHART_COLORS.CALL_COUNT);
        error_option = getEchartsOption(timeArr, data.errs, (i18n as any).t('chart.errorCount'), CHART_COLORS.ERROR_COUNT);
    } else {
        duration_option = getEchartsOption(timeArr, data.durations, (i18n as any).t('chart.responseTime'), CHART_COLORS.RESPONSE_TIME, compareData.durations, compareOptions.isCompareTime, compareOptions.compareDate, compareOptions.freshTime, compareOptions.machineIds);
        count_option = getEchartsOption(timeArr, data.counts, (i18n as any).t('chart.callCount'), CHART_COLORS.CALL_COUNT, compareData.counts, compareOptions.isCompareTime, compareOptions.compareDate, compareOptions.freshTime, compareOptions.machineIds);
        error_option = getEchartsOption(timeArr, data.errs, (i18n as any).t('chart.errorCount'), CHART_COLORS.ERROR_COUNT, compareData.errs, compareOptions.isCompareTime, compareOptions.compareDate, compareOptions.freshTime, compareOptions.machineIds);
    }

    return {
        duration: duration_option,
        count: count_option,
        error: error_option
    };
}

/**
 * Get RED Echarts configuration object
 */
export function getREDEchartsOptionObject(timeArr: any[], durationArr: any[], countArr: any[], errorArr: any[]): any {
    return {
        tooltip: {
            trigger: 'axis',
            formatter: function (params: any) {
                return tooltipFormatter(params)
            }
        },
        legend: {
            data: [(i18n as any).t('chart.responseTime'), (i18n as any).t('chart.callCount'), (i18n as any).t('chart.errorCount')]
        },
        grid: DEFAULT_GRID_CONFIG,
        toolbox: DEFAULT_TOOLBOX_CONFIG,
        calculable: true,
        xAxis: xAxisConfig(timeArr),
        yAxis: yAxisConfig(function (value: any) {
            return value;
        }),
        series: [
            {
                name: (i18n as any).t('chart.responseTime'),
                type: 'line',
                smooth: true,
                areaStyle: {
                    color: CHART_COLORS.RESPONSE_TIME,
                    opacity: 0.3
                },
                itemStyle: {
                    color: CHART_COLORS.RESPONSE_TIME
                },
                data: durationArr
            },
            {
                name: (i18n as any).t('chart.callCount'),
                type: 'line',
                smooth: true,
                areaStyle: {
                    color: CHART_COLORS.CALL_COUNT,
                    opacity: 0.3
                },
                itemStyle: {
                    color: CHART_COLORS.CALL_COUNT
                },
                data: countArr
            },
            {
                name: (i18n as any).t('chart.errorCount'),
                type: 'line',
                smooth: true,
                areaStyle: {
                    color: CHART_COLORS.ERROR_COUNT,
                    opacity: 0.3
                },
                itemStyle: {
                    color: CHART_COLORS.ERROR_COUNT
                },
                data: errorArr
            }
        ]
    };
}

/**
 * Generate pie chart configuration option
 * @param data - Data source
 * @param {String} title - Title text
 * @param {String} valueKey - Property name in data source (e.g., 'totalTime')
 * @param {String} tooltipUnit - Tooltip unit (e.g., 's')
 * @param {String} labelUnit - Label display unit (e.g., 's', 'ms')
 * @returns {Object} ECharts Option object
 */
export function getWhoCallMePieOption(data: any[], title: string, valueKey: string, tooltipUnit: string, labelUnit: string): any {
    return {
        title: {
            text: title,
            subtext: (i18n as any).t('chart.bySourceInterface'),
            top: '5px',
            left: '5px'
        },
        tooltip: {
            trigger: 'item',
            confine: false,
            extraCssText: 'max-width: none; white-space: normal; word-break: break-all;',
            formatter: function (params: any) {
                return `${params.name}：${params.value}${tooltipUnit} ${params.percent === 0 ? '<0.01' : params.percent}%`;
            }
        },
        legend: {
            top: 'bottom',
            left: 'center',
            type: 'scroll',
            padding: [5, 30, 5, 0],
            selector: ['all', 'inverse'],
        },
        series: [
            {
                name: 'data',
                type: 'pie',
                radius: ['40%', '70%'],
                itemStyle: {
                    borderRadius: 10
                },
                labelLayout: {
                    draggable: true
                },
                minShowLabelAngle: 25,
                minAngle: 0.5,
                padAngle: 0.5,
                label: {
                    // eg: '{c}s {per|{d}%}'
                    formatter: `{c}${labelUnit} {per|{d}%}`,
                    backgroundColor: '#F6F8FC',
                    borderColor: '#8C8D8E',
                    borderWidth: 1,
                    borderRadius: 4,
                    rich: {
                        b: {
                            color: '#4C5058',
                            fontSize: 12,
                            fontWeight: 'bold',
                            lineHeight: 33
                        },
                        per: {
                            color: '#fff',
                            backgroundColor: '#4C5058',
                            padding: [3, 3],
                            borderRadius: 4
                        }
                    }
                },
                labelLine: {
                    show: true
                },
                data: data.map(item => ({
                    value: item[valueKey],
                    name: item.callerServiceName + '#' + item.ifaceCaller
                }))
            }
        ]
    };
}

export function getAnalyseMapOption(timeArr: any[]): any {
    return {
        tooltip: {
            trigger: 'axis',
            formatter: function (params: any[]) {
                if (params.length === 0) {
                    return '';
                }
                params.sort((a, b) => b.data - a.data);
                let time = params[0].name.trim();
                let str = '<table style="font-size: small;padding-left: 1px">';
                str += '<tr style="font-weight: bold; text-decoration:underline; font-style: italic; color: #2d8cf0"><td>\t</td><td>Time: ' + time + '</td><td>\t</td></tr>';
                str += `<tr style="font-weight: bold;"><td>\t</td><td>${(i18n as any).t('chart.codeSegment')}</td><td>${(i18n as any).t('chart.avgDurationMs')}</td></tr>`;
                params.forEach((item) => {
                    str += '<tr>';
                    let series_name = echarts.format.truncateText(item.seriesName, 600, '', '...') + '&nbsp;'.repeat(10);
                    str += `<td>${item.marker}</td><td>${series_name}</td><td>${formatTinyNumber(2, false, item.data)}</td>`
                    str += '</tr>'
                })
                str += '</table>'
                return str
            }
        },
        legend: {
            type: 'scroll',
            padding: [5, 350, 0, 0],
            selector: ['all', 'inverse'],
            tooltip: {
                show: true,
                formatter: (name: any) => {
                    let input = name.name;
                    const spanArray = [];
                    for (let i = 0; i < input.length; i += 100) {
                        const chunk = input.slice(i, i + 100);
                        spanArray.push(`<span>${chunk}</span>`);
                    }
                    return spanArray.join('<br />');
                }
            },
            textStyle: {
                width: 100,
                overflow: 'truncate'
            },
            data: []
        },
        grid: DEFAULT_GRID_CONFIG,
        toolbox: DEFAULT_TOOLBOX_CONFIG,
        calculable: true,
        xAxis: xAxisConfig(timeArr),
        yAxis: yAxisConfig(
            function (value: any) {
                return value + 'ms';
            }
        ),
        series: [],
    };
}

function getEchartsOption(xArr: any[], yArr: any[], name: string, color: string, compareData?: any[], isCompareTime?: boolean, compareDate?: any, freshTime?: any[], machineIds?: any[]): any {
    let option: any = {
        tooltip: {
            trigger: 'axis',
            formatter: function (params: any) {
                return tooltipFormatter(params)
            }
        },
        legend: {
            data: []
        },
        grid: DEFAULT_GRID_CONFIG,
        toolbox: DEFAULT_TOOLBOX_CONFIG,
        calculable: true,
        xAxis: xAxisConfig(xArr),
        yAxis: yAxisConfig(function (value: any) {
            if (color === CHART_COLORS.RESPONSE_TIME) {
                return value + 'ms';
            } else {
                return value;
            }
        }),
        series: [
            {
                name: '',
                type: 'line',
                smooth: true,
                areaStyle: {
                    color: color,
                    opacity: 0.3
                },
                itemStyle: {
                    color: color
                },
                data: yArr
            }
        ]
    }
    // If compare data is passed, set name value based on time comparison
    if (compareData) {
        option.series[1] = {
            name: '',
            type: 'line',
            smooth: true,
            areaStyle: {
                color: '#bfb184',
                opacity: 0.3
            },
            itemStyle: {
                color: '#bfb184'
            },
            data: compareData
        }
        // If time comparison
        if (isCompareTime && freshTime && compareDate) {
            let name = freshTime[0];
            let compareName = compareDate.startDate
            if (name === compareName) compareName = compareName + '-（time overlap）'
            option.series[0].name = name
            option.series[1].name = compareName
            option.legend.data.push(name)
            option.legend.data.push(compareName)
        } else if (machineIds && machineIds.length > 1) {
            let firstNameArray = machineIds[0].split('-')
            let firstName = firstNameArray.slice(firstNameArray.length - 3, firstNameArray.length).join('-')
            let secondNameArray = machineIds[1].split('-')
            let secondName = secondNameArray.slice(secondNameArray.length - 3, secondNameArray.length).join('-')
            option.series[0].name = firstName
            option.series[1].name = secondName
            option.legend.data.push(firstName)
            option.legend.data.push(secondName)
        }
    } else {
        option.series[0].name = name
        option.legend.data.push(name)
    }
    return option
}

function tooltipFormatter(params: any[]): string {
    if (params.length === 0) {
        return '';
    }
    params.sort((a, b) => b.data - a.data);
    let time = params[0].name.trim();
    let str = '<table style="font-size: small;padding-left: 1px">';
    str += '<tr style="font-weight: bold; text-decoration:underline; font-style: italic; color: #2d8cf0"><td>\t</td><td>Time: ' + time + '</td><td>\t</td></tr>';
    params.forEach((item) => {
        str += '<tr style="font-weight: bold;">';
        let series_name = echarts.format.truncateText(item.seriesName, 600, '', '...') + '&nbsp;'.repeat(10);
        let format_value = item.color === CHART_COLORS.RESPONSE_TIME;
        if (format_value) {
            str += `<td>${item.marker}</td><td>${series_name}</td><td>${formatTinyNumber(2, false, item.data)}</td>`
        } else {
            str += `<td>${item.marker}</td><td>${series_name}</td><td>${item.data.toLocaleString().replace(/,/g, '\u2009')}</td>`
        }
        str += '</tr>'
    })
    str += '</table>'
    return str
}
