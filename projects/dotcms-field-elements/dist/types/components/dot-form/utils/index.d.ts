import '../../../stencil.core';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow, DotCMSContentTypeFieldVariable } from 'dotcms-models';
export declare const DOT_ATTR_PREFIX = "dot";
/**
 * Sets attributes to the HtmlElement from fieldVariables array
 *
 * @param HTMLElement element
 * @param DotCMSContentTypeFieldVariable fieldVariables
 */
export declare function setAttributesToTag(element: HTMLElement, fieldVariables: DotCMSContentTypeFieldVariable[]): void;
/**
 * Sets attributes with "dot" prefix to the HtmlElement passed
 *
 * @param Element element
 * @param Attr[] attributes
 */
export declare function setDotAttributesToElement(element: Element, attributes: Attr[]): void;
/**
 * Returns "Dot" attributes from all element's attributes
 *
 * @param Attr[] attributes
 * @param string[] attrException
 * @returns Attr[]
 */
export declare function getDotAttributesFromElement(attributes: Attr[], attrException: string[]): Attr[];
/**
 * Returns if a field should be displayed from a comma separated list of fields
 * @param DotCMSContentTypeField field
 * @returns boolean
 */
export declare const shouldShowField: (field: DotCMSContentTypeField, fieldsToShow: string) => boolean;
/**
 * Returns value of a Field Variable from a given key
 * @param DotCMSContentTypeFieldVariable[] fieldVariables
 * @param string key
 * @returns string
 */
export declare const getFieldVariableValue: (fieldVariables: DotCMSContentTypeFieldVariable[], key: string) => string;
/**
 * Parse a string to JSON and returns the message text
 * @param string message
 * @returns string
 */
export declare const getErrorMessage: (message: string) => string;
/**
 * Given a layout Object of fields, it returns a flat list of fields
 * @param DotCMSContentTypeLayoutRow[] layout
 * @returns DotCMSContentTypeField[]
 */
export declare const getFieldsFromLayout: (layout: DotCMSContentTypeLayoutRow[]) => DotCMSContentTypeField[];
export declare const fieldCustomProcess: {
    'DOT-KEY-VALUE': (values: string) => {
        [key: string]: string;
    };
};
export declare const fieldMap: {
    Time: (field: DotCMSContentTypeField) => JSX.Element;
    Textarea: (field: DotCMSContentTypeField) => JSX.Element;
    Text: (field: DotCMSContentTypeField) => JSX.Element;
    Tag: (field: DotCMSContentTypeField) => JSX.Element;
    Select: (field: DotCMSContentTypeField) => JSX.Element;
    Radio: (field: DotCMSContentTypeField) => JSX.Element;
    'Multi-Select': (field: DotCMSContentTypeField) => JSX.Element;
    'Key-Value': (field: DotCMSContentTypeField) => JSX.Element;
    'Date-and-Time': (field: DotCMSContentTypeField) => JSX.Element;
    'Date-Range': (field: DotCMSContentTypeField) => JSX.Element;
    Date: (field: DotCMSContentTypeField) => JSX.Element;
    Checkbox: (field: DotCMSContentTypeField) => JSX.Element;
    Binary: (field: DotCMSContentTypeField) => JSX.Element;
};
