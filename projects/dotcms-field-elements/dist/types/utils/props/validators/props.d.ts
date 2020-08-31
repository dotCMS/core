import { PropValidationInfo } from '../models/PropValidationInfo';
/**
 * Check if the value of PropValidationInfo is a string.
 *
 * @param PropValidationInfo propInfo
 */
export declare function stringValidator<T>(propInfo: PropValidationInfo<T>): void;
/**
 * Check if the value of PropValidationInfo is a valid Regular Expression.
 *
 * @param PropValidationInfo propInfo
 */
export declare function regexValidator<T>(propInfo: PropValidationInfo<T>): void;
/**
 * Check if the value of PropValidationInfo is a Number.
 *
 * @param PropValidationInfo propInfo
 */
export declare function numberValidator<T>(propInfo: PropValidationInfo<T>): void;
/**
 * Check if the value of PropValidationInfo is a valid Date, eg. yyyy-mm-dd.
 *
 * @param PropValidationInfo propInfo
 */
export declare function dateValidator<T>(propInfo: PropValidationInfo<T>): void;
/**
 * Check if the value of PropValidationInfo has two valid dates (eg. yyyy-mm-dd) and the first one should higher than the second one.
 *
 * @param PropValidationInfo propInfo
 */
export declare function dateRangeValidator<T>(propInfo: PropValidationInfo<T>): void;
/**
 * Check if the value of PropValidationInfo is a valid Time, eg. hh:mm:ss.
 *
 * @param PropValidationInfo propInfo
 */
export declare function timeValidator<T>(propInfo: PropValidationInfo<T>): void;
/**
 * Check if the value of PropValidationInfo has a valid date and time | date | time.
 * eg. 'yyyy-mm-dd hh:mm:ss' | 'yyyy-mm-dd' | 'hh:mm:ss'
 *
 * @param PropValidationInfo propInfo
 */
export declare function dateTimeValidator<T>(propInfo: PropValidationInfo<T>): void;
