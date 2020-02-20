import { h } from './core-5e49af37.js';

/**
 * Returns CSS classes object based on field Status values
 *
 * @param DotFieldStatus status
 * @param boolean isValid
 * @param boolean [required]
 * @returns DotFieldStatusClasses
 */
function getClassNames(status, isValid, required) {
    return {
        "dot-valid": isValid,
        "dot-invalid": !isValid,
        "dot-pristine": status.dotPristine,
        "dot-dirty": !status.dotPristine,
        "dot-touched": status.dotTouched,
        "dot-untouched": !status.dotTouched,
        "dot-required": required
    };
}
/**
 * Returns if it is a valid string
 *
 * @param string val
 * @returns boolean
 */
function isStringType(val) {
    return typeof val === "string" && !!val;
}
/**
 * Based on a string formatted with comma separated values, returns a label/value DotOption array
 *
 * @param string rawString
 * @returns DotOption[]
 */
function getDotOptionsFromFieldValue(rawString) {
    if (!isStringType(rawString)) {
        return [];
    }
    rawString = rawString.replace(/(?:\\[rn]|[\r\n]+)+/g, ",");
    const items = isKeyPipeValueFormatValid(rawString)
        ? rawString
            .split(",")
            .filter(item => !!item.length)
            .map(item => {
            const [label, value] = item.split("|");
            return { label, value };
        })
        : [];
    return items;
}
/**
 * Returns CSS class error to be set on main custom field
 *
 * @param boolean valid
 * @returns string
 */
function getErrorClass(valid) {
    return valid ? undefined : "dot-field__error";
}
/**
 * Prefix the hint for the id param
 *
 * @param string name
 * @returns string
 */
function getHintId(name) {
    const value = slugify(name);
    return value ? `hint-${value}` : undefined;
}
/**
 * Return cleanup dot prefixed id
 *
 * @param string name
 * @returns string
 */
function getId(name) {
    const value = slugify(name);
    return name ? `dot-${slugify(value)}` : undefined;
}
/**
 * Prefix the label for the id param
 *
 * @param string name
 * @returns string
 */
function getLabelId(name) {
    const value = slugify(name);
    return value ? `label-${value}` : undefined;
}
/**
 * Returns initial field Status, with possibility to change Valid status when needed (reset value)
 *
 * @param boolean isValid
 * @returns DotFieldStatus
 */
function getOriginalStatus(isValid) {
    return {
        dotValid: typeof isValid === "undefined" ? true : isValid,
        dotTouched: false,
        dotPristine: true
    };
}
/**
 * Returns a single string formatted as "Key|Value" separated with commas from a DotKeyValueField array
 *
 * @param DotKeyValueField[] values
 * @returns string
 */
function getStringFromDotKeyArray(values) {
    return values
        .map((item) => `${item.key}|${item.value}`)
        .join(",");
}
/**
 * Returns a copy of field Status with new changes
 *
 * @param DotFieldStatus state
 * @param { [key: string]: boolean } change
 * @returns DotFieldStatus
 */
function updateStatus(state, change) {
    return Object.assign(Object.assign({}, state), change);
}
/**
 * Returns Error tag if "show" value equals true
 *
 * @param boolean show
 * @param string message
 * @returns JSX.Element
 */
function getTagError(show, message) {
    return show && isStringType(message) ? (h("span", { class: "dot-field__error-message" }, message)) : null;
}
/**
 * Returns Hint tag if "hint" value defined
 *
 * @param string hint
 * @param string name
 * @returns JSX.Element
 */
function getTagHint(hint) {
    return isStringType(hint) ? (h("span", { class: "dot-field__hint", id: getHintId(hint) }, hint)) : null;
}
/**
 * Check if an URL is valid.
 * @param string url
 *
 * @returns boolean
 */
function isValidURL(url) {
    try {
        return !!new URL(url);
    }
    catch (e) {
        return false;
    }
}
/**
 * Check if the fileName extension is part of the allowed extensions
 *
 * @param string fileName
 * @param string[] allowedExtensions
 *
 * @returns boolean
 */
function isFileAllowed(fileName, allowedExtensions) {
    let allowedExtensionsArray = allowedExtensions.split(",");
    allowedExtensionsArray = allowedExtensionsArray.map((item) => item.trim());
    const extension = fileName
        ? fileName.substring(fileName.indexOf("."), fileName.length)
        : "";
    return (allowAnyFile(allowedExtensionsArray) ||
        allowedExtensionsArray.includes(extension));
}
function allowAnyFile(allowedExtensions) {
    return allowedExtensions[0] === "" || allowedExtensions.includes("*");
}
function slugify(text) {
    return text
        ? text
            .toString()
            .toLowerCase()
            .replace(/\s+/g, "-") // Replace spaces with -
            .replace(/[^\w\-]+/g, "") // Remove all non-word chars
            .replace(/\-\-+/g, "-") // Replace multiple - with single -
            .replace(/^-+/, "") // Trim - from start of text
            .replace(/-+$/, "") // Trim - from end of text
        : null;
}
function isKeyPipeValueFormatValid(rawString) {
    const regex = /([^|,]*)\|([^|,]*)/;
    const items = rawString.split(",");
    let valid = true;
    for (let i = 0, total = items.length; i < total; i++) {
        if (!regex.test(items[i])) {
            valid = false;
            break;
        }
    }
    return valid;
}

const DATE_REGEX = new RegExp('^\\d\\d\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])');
const TIME_REGEX = new RegExp('^(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])$');
/**
 * Check if date is valid, returns a valid date string, otherwise null.
 *
 * @param string date
 * @returns string
 */
function dotValidateDate(date) {
    return DATE_REGEX.test(date) ? date : null;
}
/**
 * Check if time is valid, returns a valid time string, otherwise null.
 *
 * @param string time
 * @returns string
 */
function dotValidateTime(time) {
    return TIME_REGEX.test(time) ? time : null;
}
/**
 * Parse a data-time string that can contains 'date time' | date | time.
 *
 * @param string data
 * @returns DotDateSlot
 */
function dotParseDate(data) {
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
function isValidDateSlot(dateSlot, rawData) {
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
function isValidFullDateSlot(dateSlot) {
    return !!dateSlot.date && !!dateSlot.time;
}
/**
 * Check is there as least one valid value in the DotDateSlot
 *
 * @param DotDateSlot dateSlot
 * @returns boolean
 */
function isValidPartialDateSlot(dateSlot) {
    return !!dateSlot.date || !!dateSlot.time;
}

class DotFieldPropError extends Error {
    constructor(propInfo, expectedType) {
        super(`Warning: Invalid prop "${propInfo.name}" of type "${typeof propInfo.value}" supplied to "${propInfo.field.type}" with the name "${propInfo.field.name}", expected "${expectedType}".
Doc Reference: https://github.com/dotCMS/core-web/blob/master/projects/dotcms-field-elements/src/components/${propInfo.field.type}/readme.md`);
        this.propInfo = propInfo;
    }
    getProps() {
        return Object.assign({}, this.propInfo);
    }
}

/**
 * Check if the value of PropValidationInfo is a string.
 *
 * @param PropValidationInfo propInfo
 */
function stringValidator(propInfo) {
    if (typeof propInfo.value !== 'string') {
        throw new DotFieldPropError(propInfo, 'string');
    }
}
/**
 * Check if the value of PropValidationInfo is a valid Regular Expression.
 *
 * @param PropValidationInfo propInfo
 */
function regexValidator(propInfo) {
    try {
        RegExp(propInfo.value.toString());
    }
    catch (e) {
        throw new DotFieldPropError(propInfo, 'valid regular expression');
    }
}
/**
 * Check if the value of PropValidationInfo is a Number.
 *
 * @param PropValidationInfo propInfo
 */
function numberValidator(propInfo) {
    if (isNaN(Number(propInfo.value))) {
        throw new DotFieldPropError(propInfo, 'Number');
    }
}
/**
 * Check if the value of PropValidationInfo is a valid Date, eg. yyyy-mm-dd.
 *
 * @param PropValidationInfo propInfo
 */
function dateValidator(propInfo) {
    if (!dotValidateDate(propInfo.value.toString())) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
}
const areRangeDatesValid = (start, end, propInfo) => {
    if (start > end) {
        throw new DotFieldPropError(propInfo, 'Date');
    }
};
/**
 * Check if the value of PropValidationInfo has two valid dates (eg. yyyy-mm-dd) and the first one should higher than the second one.
 *
 * @param PropValidationInfo propInfo
 */
function dateRangeValidator(propInfo) {
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
function timeValidator(propInfo) {
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
function dateTimeValidator(propInfo) {
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

const PROP_VALIDATION_HANDLING = {
    date: dateValidator,
    dateRange: dateRangeValidator,
    dateTime: dateTimeValidator,
    number: numberValidator,
    options: stringValidator,
    regexCheck: regexValidator,
    step: stringValidator,
    string: stringValidator,
    time: timeValidator,
    type: stringValidator,
    accept: stringValidator
};
const FIELDS_DEFAULT_VALUE = {
    options: '',
    regexCheck: '',
    value: '',
    min: '',
    max: '',
    step: '',
    type: 'text',
    accept: null
};
function validateProp(propInfo, validatorType) {
    if (!!propInfo.value) {
        PROP_VALIDATION_HANDLING[validatorType || propInfo.name](propInfo);
    }
}
function getPropInfo(element, propertyName) {
    return {
        value: element[propertyName],
        name: propertyName,
        field: {
            name: element['name'],
            type: element['el'].tagName.toLocaleLowerCase()
        }
    };
}
function checkProp(component, propertyName, validatorType) {
    const proInfo = getPropInfo(component, propertyName);
    try {
        validateProp(proInfo, validatorType);
        return component[propertyName];
    }
    catch (error) {
        console.warn(error.message);
        return FIELDS_DEFAULT_VALUE[propertyName];
    }
}

export { getId as a, getClassNames as b, checkProp as c, getHintId as d, getErrorClass as e, getTagHint as f, getOriginalStatus as g, getTagError as h, isStringType as i, getStringFromDotKeyArray as j, getDotOptionsFromFieldValue as k, isFileAllowed as l, dotParseDate as m, isValidURL as n, getLabelId as o, updateStatus as u };
