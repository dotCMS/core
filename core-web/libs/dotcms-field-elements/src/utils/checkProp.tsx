import { PropValidationInfo } from './props/models';
import {
    dateValidator,
    dateTimeValidator,
    numberValidator,
    stringValidator,
    regexValidator,
    timeValidator,
    dateRangeValidator
} from './props/validators';

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

function validateProp<PropType>(
    propInfo: PropValidationInfo<PropType>,
    validatorType?: string
): void {
    if (propInfo.value) {
        PROP_VALIDATION_HANDLING[validatorType || propInfo.name](propInfo);
    }
}

function getPropInfo<ComponentClass, PropType>(
    element: ComponentClass,
    propertyName: string
): PropValidationInfo<PropType> {
    return {
        value: element[propertyName],
        name: propertyName,
        field: {
            name: element['name'],
            type: element['el'].tagName.toLocaleLowerCase()
        }
    };
}

export function checkProp<ComponentClass, PropType>(
    component: ComponentClass,
    propertyName: string,
    validatorType?: string
): string {
    const proInfo = getPropInfo<ComponentClass, PropType>(component, propertyName);

    try {
        validateProp<PropType>(proInfo, validatorType);
        return component[propertyName];
    } catch (error) {
        console.warn(error.message);
        return FIELDS_DEFAULT_VALUE[propertyName];
    }
}
