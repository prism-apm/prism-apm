/**
 * Format a number to a string with a specified number of decimal places.
 * If the number is too small, return a threshold string.
 * @param toFixed - The number of decimal places to keep.
 * @param percent - Whether to format as a percentage.
 * @param num - The number to format.
 * @returns {string} The formatted number or threshold string.
 */
export const formatTinyNumber = (toFixed: number, percent: boolean, num: number | string): string => {
    // First process the number and keep the specified number of decimal places
    const fixedNum = Number(num).toFixed(toFixed);
    const parsedNum = parseFloat(fixedNum);

    // Dynamically generate a threshold, the number of zeros matches toFixed
    const thresholdValue = '0.' + '0'.repeat(toFixed - 1) + '1';
    const threshold = percent ? `<${thresholdValue}%` : `<${thresholdValue}`;

    // Handle the case where toFixed is 0
    if (Number(num) === 0) {
        return percent ? `${fixedNum}%` : fixedNum;
    }

    // Determine if the truncated value is <= 0
    if (parsedNum <= 0) {
        return threshold;
    }

    // Return the normally truncated number, with a percentage sign (if needed)
    return percent ? `${fixedNum}%` : fixedNum;
};
