import { dateValidator, dateTimeValidator, numberValidator, stringValidator, regexValidator, timeValidator, dateRangeValidator } from './props/validators';
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
export function checkProp(component, propertyName, validatorType) {
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
