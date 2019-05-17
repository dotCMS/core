import { PropValidationInfo } from '../models/PropValidationInfo';
import DotFieldPropError from '../DotFieldPropError';
import { dotValidateDate, dotValidateTime, dotParseDate, isValidDateSlot } from './date';

export function stringValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (typeof propInfo.value !== 'string') {
        throw new DotFieldPropError(propInfo, 'string');
    }
}

export function regexValidator<T>(propInfo: PropValidationInfo<T>): void {
    try {
        RegExp(propInfo.value.toString());
    } catch (e) {
        throw new DotFieldPropError(propInfo, 'valid regular expression');
    }
}

export function numberValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (isNaN(Number(propInfo.value))) {
        throw new DotFieldPropError(propInfo, 'Number');
    }
}

export function dateValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (!dotValidateDate(propInfo.value.toString())) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
}

export function timeValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (!dotValidateTime(propInfo.value.toString())) {
        throw new DotFieldPropError(propInfo, 'Time');
    }
}

export function dateTimeValidator<T>(propInfo: PropValidationInfo<T>): void {
    if (typeof propInfo.value === 'string') {
        const dateSlot = dotParseDate(propInfo.value.toString());
        if (!isValidDateSlot(dateSlot)) {
            throw new DotFieldPropError(propInfo, 'Date/Time');
        }
    } else {
        throw new DotFieldPropError(propInfo, 'Date/Time');
    }
}
