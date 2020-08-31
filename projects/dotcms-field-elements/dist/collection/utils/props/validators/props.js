import DotFieldPropError from '../DotFieldPropError';
import { dotValidateDate, dotValidateTime, dotParseDate, isValidDateSlot } from './date';
export function stringValidator(propInfo) {
    if (typeof propInfo.value !== 'string') {
        throw new DotFieldPropError(propInfo, 'string');
    }
}
export function regexValidator(propInfo) {
    try {
        RegExp(propInfo.value.toString());
    }
    catch (e) {
        throw new DotFieldPropError(propInfo, 'valid regular expression');
    }
}
export function numberValidator(propInfo) {
    if (isNaN(Number(propInfo.value))) {
        throw new DotFieldPropError(propInfo, 'Number');
    }
}
export function dateValidator(propInfo) {
    if (!dotValidateDate(propInfo.value.toString())) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
}
const areRangeDatesValid = (start, end, propInfo) => {
    if (start > end) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
};
export function dateRangeValidator(propInfo) {
    const [start, end] = propInfo.value.toString().split(',');
    if (!dotValidateDate(start) || !dotValidateDate(end)) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
    areRangeDatesValid(new Date(start), new Date(end), propInfo);
}
export function timeValidator(propInfo) {
    if (!dotValidateTime(propInfo.value.toString())) {
        throw new DotFieldPropError(propInfo, 'Time');
    }
}
export function dateTimeValidator(propInfo) {
    if (typeof propInfo.value === 'string') {
        const dateSlot = dotParseDate(propInfo.value);
        if (!isValidDateSlot(dateSlot, propInfo.value)) {
            throw new DotFieldPropError(propInfo, 'Date/Time');
        }
    }
    else {
        throw new DotFieldPropError(propInfo, 'Date/Time');
    }
}
