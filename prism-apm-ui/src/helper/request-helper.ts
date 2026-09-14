import { DEFAULT_MACHINE_ID } from "@/constants/default-vals";
import {COMPARE_MODES} from "@/constants/compare-mode";

/**
 * Determine if comparison data needs to be requested
 * Based on comparison mode and related parameters to determine if comparison data needs to be retrieved
 *
 * @param {string} compareMode - Comparison mode, can be time comparison or host comparison
 * @param compareDate - Comparison date, used in time comparison mode
 * @param machineIds - Array of machine IDs, used in host comparison mode
 * @returns Returns true if comparison conditions are met, otherwise returns false
 */
export function needRequestCompareData(compareMode: string, compareDate: any, machineIds: string[]): boolean {
    return readyForRequestTimeCompareData(compareMode, compareDate) || readyForRequestMachineCompareData(compareMode, machineIds);
}

export function readyForRequestMachineCompareData(compareMode: string, machineIds: string[]): boolean {
    return (isMachineCompareMode(compareMode) && machineIds.length > 1);
}

export function readyForRequestTimeCompareData(compareMode: string, compareDate: any): boolean {
    return (isTimeCompareMode(compareMode) && compareDate);
}

export function isMachineCompareMode(compareMode: string): boolean {
    return compareMode === COMPARE_MODES.HOST;
}

export function isTimeCompareMode(compareMode: string): boolean {
    return compareMode === COMPARE_MODES.TIME;
}

/**
 * Select appropriate machine ID for API parameters based on comparison mode and related parameters
 * @param {string} compareMode - Comparison mode, can be HOST or TIME etc.
 * @param {any} compareDate - Date parameter for time comparison
 * @param isCompareApi - Whether it is a comparison API call
 * @param {string} machineId - Current machine ID, may be a string or array
 * @param machineIds - Array of machine IDs, containing multiple machine IDs
 * @returns {string|undefined} Returns the selected machine ID, or undefined if conditions are not met
 */
export function pickMachineIdForApiParams(compareMode: string, compareDate: any, isCompareApi: boolean, machineId: string, machineIds: string[]): string | undefined {
    // Handle machine ID selection logic for host comparison mode
    if (DEFAULT_MACHINE_ID !== machineIds[0] && compareMode === COMPARE_MODES.HOST) {
        if (isCompareApi) {
            return machineIds[1];
        } else {
            return machineIds[0];
        }
    } else if (DEFAULT_MACHINE_ID !== machineId && compareMode === COMPARE_MODES.TIME && compareDate) {
        // Handle machine ID selection logic for time comparison mode
        return machineId;
    }
}
