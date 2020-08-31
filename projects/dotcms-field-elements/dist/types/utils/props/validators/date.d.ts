import { DotDateSlot } from '../../../models';
/**
 * Check if date is valid, returns a valid date string, otherwise null.
 *
 * @param string date
 * @returns string
 */
export declare function dotValidateDate(date: string): string;
/**
 * Check if time is valid, returns a valid time string, otherwise null.
 *
 * @param string time
 * @returns string
 */
export declare function dotValidateTime(time: string): string;
/**
 * Parse a data-time string that can contains 'date time' | date | time.
 *
 * @param string data
 * @returns DotDateSlot
 */
export declare function dotParseDate(data: string): DotDateSlot;
/**
 * Check if DotDateSlot is valid based on the raw data.
 *
 * @param DotDateSlot dateSlot
 * @param string rawData
 */
export declare function isValidDateSlot(dateSlot: DotDateSlot, rawData: string): boolean;
