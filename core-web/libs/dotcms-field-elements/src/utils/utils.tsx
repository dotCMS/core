import { DotOption, DotFieldStatus, DotFieldStatusClasses, DotKeyValueField } from '../models';
import { h } from '@stencil/core';

/**
 * Returns CSS classes object based on field Status values
 *
 * @param DotFieldStatus status
 * @param boolean isValid
 * @param boolean [required]
 * @returns DotFieldStatusClasses
 */
export function getClassNames(
    status: DotFieldStatus,
    isValid: boolean,
    required?: boolean
): DotFieldStatusClasses {
    return {
        'dot-valid': isValid,
        'dot-invalid': !isValid,
        'dot-pristine': status.dotPristine,
        'dot-dirty': !status.dotPristine,
        'dot-touched': status.dotTouched,
        'dot-untouched': !status.dotTouched,
        'dot-required': required
    };
}

/**
 * Returns if it is a valid string
 *
 * @param string val
 * @returns boolean
 */
export function isStringType(val: string): boolean {
    return typeof val === 'string' && !!val;
}

/**
 * Based on a string formatted with comma separated values, returns a label/value DotOption array
 *
 * @param string rawString
 * @returns DotOption[]
 */
export function getDotOptionsFromFieldValue(rawString: string): DotOption[] {
    if (!isStringType(rawString)) {
        return [];
    }

    rawString = rawString.replace(/(?:\\[rn]|[\r\n]+)+/g, ',');

    const items = isKeyPipeValueFormatValid(rawString)
        ? rawString
              .split(',')
              .filter((item) => !!item.length)
              .map((item) => {
                  const [label, value] = item.split('|');
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
export function getErrorClass(valid: boolean): string {
    return valid ? undefined : 'dot-field__error';
}

/**
 * Prefix the hint for the id param
 *
 * @param string name
 * @returns string
 */
export function getHintId(name: string): string {
    const value = slugify(name);
    return value ? `hint-${value}` : undefined;
}

/**
 * Return cleanup dot prefixed id
 *
 * @param string name
 * @returns string
 */
export function getId(name: string): string {
    const value = slugify(name);
    return name ? `dot-${slugify(value)}` : undefined;
}

/**
 * Prefix the label for the id param
 *
 * @param string name
 * @returns string
 */
export function getLabelId(name: string): string {
    const value = slugify(name);
    return value ? `label-${value}` : undefined;
}

/**
 * Returns initial field Status, with possibility to change Valid status when needed (reset value)
 *
 * @param boolean isValid
 * @returns DotFieldStatus
 */
export function getOriginalStatus(isValid?: boolean): DotFieldStatus {
    return {
        dotValid: typeof isValid === 'undefined' ? true : isValid,
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
export function getStringFromDotKeyArray(values: DotKeyValueField[]): string {
    return values.map((item: DotKeyValueField) => `${item.key}|${item.value}`).join(',');
}

/**
 * Returns a copy of field Status with new changes
 *
 * @param DotFieldStatus state
 * @param { [key: string]: boolean } change
 * @returns DotFieldStatus
 */
export function updateStatus(
    state: DotFieldStatus,
    change: { [key: string]: boolean }
): DotFieldStatus {
    return {
        ...state,
        ...change
    };
}

/**
 * Returns Error tag if "show" value equals true
 *
 * @param boolean show
 * @param string message
 * @returns JSX.Element
 */
export function getTagError(show: boolean, message: string) {
    return show && isStringType(message) ? (
        <span class="dot-field__error-message">{message}</span>
    ) : null;
}

/**
 * Returns Hint tag if "hint" value defined
 *
 * @param string hint
 * @param string name
 * @returns JSX.Element
 */
export function getTagHint(hint: string) {
    return isStringType(hint) ? (
        <span class="dot-field__hint" id={getHintId(hint)}>
            {hint}
        </span>
    ) : null;
}

/**
 * Check if an URL is valid.
 * @param string url
 *
 * @returns boolean
 */
export function isValidURL(url: string): boolean {
    try {
        return !!new URL(url);
    } catch (e) {
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
export function isFileAllowed(fileName: string, allowedExtensions: string): boolean {
    let allowedExtensionsArray = allowedExtensions.split(',');
    allowedExtensionsArray = allowedExtensionsArray.map((item: string) => item.trim());
    const extension = fileName ? fileName.substring(fileName.indexOf('.'), fileName.length) : '';

    return allowAnyFile(allowedExtensionsArray) || allowedExtensionsArray.includes(extension);
}

function allowAnyFile(allowedExtensions: string[]): boolean {
    return allowedExtensions[0] === '' || allowedExtensions.includes('*');
}

function slugify(text: string): string {
    return text
        ? text
              .toString()
              .toLowerCase()
              .replace(/\s+/g, '-') // Replace spaces with -
              .replace(/[^\w\-]+/g, '') // Remove all non-word chars
              .replace(/\-\-+/g, '-') // Replace multiple - with single -
              .replace(/^-+/, '') // Trim - from start of text
              .replace(/-+$/, '') // Trim - from end of text
        : null;
}

function isKeyPipeValueFormatValid(rawString: string): boolean {
    const regex = /([^|,]*)\|([^|,]*)/;
    const items = rawString.split(',');
    let valid = true;

    for (let i = 0, total = items.length; i < total; i++) {
        if (!regex.test(items[i])) {
            valid = false;
            break;
        }
    }
    return valid;
}
