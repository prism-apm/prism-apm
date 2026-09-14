import { isTimeCompareMode } from "@/helper/request-helper";

/**
 * @param {string} compareMode compare by host or time
 * @param {any} compareDate
 * @param {any[]} freshTime
 * @param {any[]} machineIds
 */
export default class EchartsRedCompareOption {
    isCompareTime: boolean;
    compareDate: any;
    freshTime: any[];
    machineIds: any[];

    constructor(compareMode: string, compareDate: any, freshTime: any[], machineIds: any[]) {
        this.isCompareTime = isTimeCompareMode(compareMode);
        this.compareDate = compareDate;
        this.freshTime = freshTime;
        this.machineIds = machineIds;
    }
}
