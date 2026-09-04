import {
    dateValidator,
    dateTimeValidator,
    numberValidator,
    stringValidator,
    regexValidator,
    timeValidator,
    dateRangeValidator
} from './props/validators';
import { PropValidationInfo } from './props/models';

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

/**
 * The fallback each prop takes when its value fails validation.
 *
 * `accept`'s null says "no restriction" rather than "empty pattern", which is why the values are
 * `string | null` and {@link checkProp} reports the same. No caller validates `accept` today, so
 * that arm is unreached — but the type has to admit it.
 */
const FIELDS_DEFAULT_VALUE: Record<string, string | null | undefined> = {
    options: '',
    regexCheck: '',
    value: '',
    min: '',
    max: '',
    step: '',
    type: 'text',
    accept: null
};

/** A component seen as a bag of props, which is all these helpers need it to be. */
type PropBag = Record<string, unknown> & { el: HTMLElement };

function validateProp<PropType>(
    propInfo: PropValidationInfo<PropType>,
    validatorType?: string
): void {
    if (!!propInfo.value) {
        // Indexed through a widened view: the key is a prop name resolved at runtime, so it cannot
        // be a `keyof` the handler map. An unrecognised one yields `undefined` and throws on call,
        // which `checkProp` already catches and turns into the prop's default — unchanged.
        const validators = PROP_VALIDATION_HANDLING as unknown as Record<
            string,
            (info: PropValidationInfo<PropType>) => void
        >;

        validators[validatorType || propInfo.name](propInfo);
    }
}

function getPropInfo<ComponentClass, PropType>(
    element: ComponentClass,
    propertyName: string
): PropValidationInfo<PropType> {
    const props = element as unknown as PropBag;

    return {
        value: props[propertyName] as PropType,
        name: propertyName,
        field: {
            name: props['name'] as string,
            type: props['el'].tagName.toLocaleLowerCase()
        }
    };
}

export function checkProp<ComponentClass, PropType>(
    component: ComponentClass,
    propertyName: string,
    validatorType?: string
): string | null | undefined {
    const proInfo = getPropInfo<ComponentClass, PropType>(component, propertyName);

    try {
        validateProp<PropType>(proInfo, validatorType);

        return (component as unknown as PropBag)[propertyName] as string;
    } catch (error) {
        // `unknown` in a catch clause under `strict`. Narrowed rather than asserted, so a thrown
        // non-Error still logs something instead of `undefined`.
        console.warn(error instanceof Error ? error.message : String(error));

        return FIELDS_DEFAULT_VALUE[propertyName];
    }
}
