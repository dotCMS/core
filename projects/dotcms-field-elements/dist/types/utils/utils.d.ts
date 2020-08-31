import '../stencil.core';
import { DotOption, DotFieldStatus, DotFieldStatusClasses, DotKeyValueField } from '../models';
/**
 * Returns CSS classes object based on field Status values
 *
 * @param DotFieldStatus status
 * @param boolean isValid
 * @param boolean [required]
 * @returns DotFieldStatusClasses
 */
export declare function getClassNames(status: DotFieldStatus, isValid: boolean, required?: boolean): DotFieldStatusClasses;
/**
 * Returns if it is a valid string
 *
 * @param string val
 * @returns boolean
 */
export declare function isStringType(val: string): boolean;
/**
 * Based on a string formatted with comma separated values, returns a label/value DotOption array
 *
 * @param string rawString
 * @returns DotOption[]
 */
export declare function getDotOptionsFromFieldValue(rawString: string): DotOption[];
/**
 * Returns CSS class error to be set on main custom field
 *
 * @param boolean valid
 * @returns string
 */
export declare function getErrorClass(valid: boolean): string;
/**
 * Prefix the hint for the id param
 *
 * @param string name
 * @returns string
 */
export declare function getHintId(name: string): string;
/**
 * Return cleanup dot prefixed id
 *
 * @param string name
 * @returns string
 */
export declare function getId(name: string): string;
/**
 * Prefix the label for the id param
 *
 * @param string name
 * @returns string
 */
export declare function getLabelId(name: string): string;
/**
 * Returns initial field Status, with possibility to change Valid status when needed (reset value)
 *
 * @param boolean isValid
 * @returns DotFieldStatus
 */
export declare function getOriginalStatus(isValid?: boolean): DotFieldStatus;
/**
 * Returns a single string formatted as "Key|Value" separated with commas from a DotKeyValueField array
 *
 * @param DotKeyValueField[] values
 * @returns string
 */
export declare function getStringFromDotKeyArray(values: DotKeyValueField[]): string;
/**
 * Returns a copy of field Status with new changes
 *
 * @param DotFieldStatus state
 * @param { [key: string]: boolean } change
 * @returns DotFieldStatus
 */
export declare function updateStatus(state: DotFieldStatus, change: {
    [key: string]: boolean;
}): DotFieldStatus;
/**
 * Returns Error tag if "show" value equals true
 *
 * @param boolean show
 * @param string message
 * @returns JSX.Element
 */
export declare function getTagError(show: boolean, message: string): JSX.Element;
/**
 * Returns Hint tag if "hint" value defined
 *
 * @param string hint
 * @param string name
 * @returns JSX.Element
 */
export declare function getTagHint(hint: string): JSX.Element;
/**
 * Check if an URL is valid.
 * @param string url
 *
 * @returns boolean
 */
export declare function isValidURL(url: string): boolean;
/**
 * Check if the fileName extension is part of the allowed extensions
 *
 * @param string fileName
 * @param string[] allowedExtensions
 *
 * @returns boolean
 */
export declare function isFileAllowed(fileName: string, allowedExtensions: string): boolean;
