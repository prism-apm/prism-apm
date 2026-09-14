import dayjs from 'dayjs';

/**
 * Format date object
 * @param date - date object
 * @param hasTime - whether to include the time part
 */
export const formateDate = (date: Date | string | number, hasTime: boolean = false): string => {
    const format = hasTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD';
    let dateObj: Date = date instanceof Date ? date : new Date(date);
    return dayjs(dateObj).format(format);
}

/**
 * Get the minute-level precision of the specified time
 * @param dateString - time string
 * @returns - time string
 */
export const truncateTimeStrToMinute = (dateString: string | number | Date): string | null => {
    // 1. Convert the string to a Date object
    const date = new Date(dateString);

    // 2. Verify that the conversion was successful. If the string format is incorrect, an Invalid Date will be returned.
    // isNaN(date.getTime()) is the standard method for judging Invalid Date.
    if (isNaN(date.getTime())) {
        console.error("The input string cannot be parsed as a valid date:", dateString);
        return null;
    }

    // 3. Set seconds and milliseconds to 0. This will directly modify the date object.
    date.setSeconds(0, 0);

    // 4. Return the modified Date object
    return formateDate(date, true);
}

/**
 * Get the date of a pastime point
 * @param type - time type, supporting 'days' (days) or 'minutes' (minutes)
 * @param value - value, representing how many days or minutes ago
 * @param hasTime - whether to include the time part
 * @returns - formatted time string
 */
export const getPastDate = (type: 'days' | 'minutes', value: number, hasTime?: boolean): string => {
    let date = new Date();

    if (type === 'days') {
        date.setDate(date.getDate() - value);
    } else if (type === 'minutes') {
        date.setMinutes(date.getMinutes() - value);
    }

    return formateDate(date, hasTime);
}

/**
 * Get the current day of the week
 * @returns - the number representing the day of the week (1-7), where 1 represents Monday and 7 represents Sunday
 */
export const getThisWeekDay = (): number => {
    let date = new Date();
    // getDay() returns 0-6, where 0 represents Sunday
    // Convert 0 (Sunday) to 7 to match the representation of Monday to Sunday as 1-7
    return date.getDay() || 7;
}


/**
 * Get the date part of the specified date
 * @param date - date object
 * @returns - formatted date string, format is 'YYYY-MM-DD'
 */
export const getDatePart = (date: Date | string | number): string => {
    const format = 'YYYY-MM-DD';
    let dateObj: Date = date instanceof Date ? date : new Date(date);
    return dayjs(dateObj).format(format);
}
/**
 * Get the time part of the specified date
 * @param date - date object
 * @returns - formatted time string, format is 'hh-mm-ss'
 */
export const getTimePart = (date: Date | string | number): string => {
    const format = 'HH:mm:ss';
    let dateObj: Date = date instanceof Date ? date : new Date(date);
    return dayjs(dateObj).format(format);
}

/**
 * Calculate the date range after passing the same duration from the specified start date based on the time difference between the start time and end time
 * @param startTime - start time string, used as the starting point for calculating the time difference
 * @param endTime - end time string, used as the endpoint for calculating the time difference
 * @param startDate - start date string, used as the actual new starting point for calculation
 * @returns - object containing startDate and endDate
 */
export const calculateDateRangeByDiff = (startTime: string, endTime: string, startDate: string): { startDate: string, endDate: string } => {
    // Calculate the time interval of endTime - startTime
    let diff = new Date(endTime).getTime() - new Date(startTime).getTime();
    let date = new Date(startDate);
    date.setMilliseconds(date.getMilliseconds() + diff);
    let lastEndTime = formateDate(date, true);
    let newStartDate = formateDate(new Date(startDate), true);
    return {
        startDate: newStartDate,
        endDate: lastEndTime
    }
}
