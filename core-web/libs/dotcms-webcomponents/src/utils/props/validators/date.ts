import { DotDateSlot } from '../../../models';

const DATE_REGEX = new RegExp('^\\d\\d\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])');
const TIME_REGEX = new RegExp('^(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])$');

/**
 * Check if date is valid, returns a valid date string, otherwise null.
 *
 * @param string date
 * @returns string
 */
export function dotValidateDate(date: string): string {
    return DATE_REGEX.test(date) ? date : null;
}

/**
 * Check if time is valid, returns a valid time string, otherwise null.
 *
 * @param string time
 * @returns string
 */
export function dotValidateTime(time: string): string {
    return TIME_REGEX.test(time) ? time : null;
}

/**
 * Parse a data-time string that can contains 'date time' | date | time.
 *
 * @param string data
 * @returns DotDateSlot
 */
export function dotParseDate(data: string): DotDateSlot {
    const [dateOrTime, time] = data ? data.split(' ') : '';
    return {
        date: dotValidateDate(dateOrTime),
        time: dotValidateTime(time) || dotValidateTime(dateOrTime)
    };
}

/**
 * Check if DotDateSlot is valid based on the raw data.
 *
 * @param DotDateSlot dateSlot
 * @param string rawData
 */
export function isValidDateSlot(dateSlot: DotDateSlot, rawData: string): boolean {
    return !!rawData
        ? rawData.split(' ').length > 1
            ? isValidFullDateSlot(dateSlot)
            : isValidPartialDateSlot(dateSlot)
        : false;
}

/**
 * Check if a DotDateSlot have date and time set
 *
 * @param DotDateSlot dateSlot
 * @returns boolean
 */
function isValidFullDateSlot(dateSlot: DotDateSlot): boolean {
    return !!dateSlot.date && !!dateSlot.time;
}

/**
 * Check is there as least one valid value in the DotDateSlot
 *
 * @param DotDateSlot dateSlot
 * @returns boolean
 */
function isValidPartialDateSlot(dateSlot: DotDateSlot): boolean {
    return !!dateSlot.date || !!dateSlot.time;
}
