const DATE_REGEX = new RegExp('^\\d\\d\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])');
const TIME_REGEX = new RegExp('^(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])$');
export function dotValidateDate(date) {
    return DATE_REGEX.test(date) ? date : null;
}
export function dotValidateTime(time) {
    return TIME_REGEX.test(time) ? time : null;
}
export function dotParseDate(data) {
    const [dateOrTime, time] = data ? data.split(' ') : '';
    return {
        date: dotValidateDate(dateOrTime),
        time: dotValidateTime(time) || dotValidateTime(dateOrTime)
    };
}
export function isValidDateSlot(dateSlot, rawData) {
    return !!rawData
        ? rawData.split(' ').length > 1
            ? isValidFullDateSlot(dateSlot)
            : isValidPartialDateSlot(dateSlot)
        : false;
}
function isValidFullDateSlot(dateSlot) {
    return !!dateSlot.date && !!dateSlot.time;
}
function isValidPartialDateSlot(dateSlot) {
    return !!dateSlot.date || !!dateSlot.time;
}
