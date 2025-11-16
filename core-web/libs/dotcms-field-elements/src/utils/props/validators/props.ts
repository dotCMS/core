import { dotValidateDate, dotValidateTime, dotParseDate, isValidDateSlot } from './date';

import DotFieldPropError from '../DotFieldPropError';
import { PropValidationInfo } from '../models/PropValidationInfo';

/**
 * Check if the value of PropValidationInfo is a string.
 *
 * @param PropValidationInfo propInfo
 */
export function stringValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (typeof propInfo.value !== 'string') {
        throw new DotFieldPropError(propInfo, 'string');
    }
}

/**
 * Check if the value of PropValidationInfo is a valid Regular Expression.
 *
 * @param PropValidationInfo propInfo
 */
export function regexValidator<T>(propInfo: PropValidationInfo<T>): void {
    try {
        RegExp(propInfo.value.toString());
    } catch (e) {
        throw new DotFieldPropError(propInfo, 'valid regular expression');
    }
}

/**
 * Check if the value of PropValidationInfo is a Number.
 *
 * @param PropValidationInfo propInfo
 */
export function numberValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (isNaN(Number(propInfo.value))) {
        throw new DotFieldPropError(propInfo, 'Number');
    }
}

/**
 * Check if the value of PropValidationInfo is a valid Date, eg. yyyy-mm-dd.
 *
 * @param PropValidationInfo propInfo
 */
export function dateValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (!dotValidateDate(propInfo.value.toString())) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
}

const areRangeDatesValid = <T>(start: Date, end: Date, propInfo: PropValidationInfo<T>): void => {
    if (start > end) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
};

/**
 * Check if the value of PropValidationInfo has two valid dates (eg. yyyy-mm-dd) and the first one should higher than the second one.
 *
 * @param PropValidationInfo propInfo
 */
export function dateRangeValidator<T>(propInfo: PropValidationInfo<T>): void {
    const [start, end] = propInfo.value.toString().split(',');
    if (!dotValidateDate(start) || !dotValidateDate(end)) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
    areRangeDatesValid(new Date(start), new Date(end), propInfo);
}

/**
 * Check if the value of PropValidationInfo is a valid Time, eg. hh:mm:ss.
 *
 * @param PropValidationInfo propInfo
 */
export function timeValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (!dotValidateTime(propInfo.value.toString())) {
        throw new DotFieldPropError(propInfo, 'Time');
    }
}

/**
 * Check if the value of PropValidationInfo has a valid date and time | date | time.
 * eg. 'yyyy-mm-dd hh:mm:ss' | 'yyyy-mm-dd' | 'hh:mm:ss'
 *
 * @param PropValidationInfo propInfo
 */
export function dateTimeValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (typeof propInfo.value === 'string') {
        const dateSlot = dotParseDate(propInfo.value);
        if (!isValidDateSlot(dateSlot, propInfo.value)) {
            throw new DotFieldPropError(propInfo, 'Date/Time');
        }
    } else {
        throw new DotFieldPropError(propInfo, 'Date/Time');
    }
}
