import { emptyAllInnerLists } from "@/utils/collection-utils";
import { makeChartDisplayNodata } from "@/utils/echarts-utils";
import Vue from 'vue'

export function isEmptyResponse(res: any): boolean {
    return !res.data || res.data.length === 0 || res.total === 0;
}

export function handleEmptyResponse(needEmptyList: any[][], needEmptyChartIds: string[]): void {
    console.info('No data return.');
    emptyAllInnerLists(needEmptyList);
    needEmptyChartIds.forEach(chartId => {
        makeChartDisplayNodata(chartId);
    });
}

export function handleErrorResponse(err: any, defaultMsg: string): void {
    console.error(defaultMsg, err);
    let message: string = err.err || defaultMsg;
    Vue.prototype.$Message.error(message);
}
