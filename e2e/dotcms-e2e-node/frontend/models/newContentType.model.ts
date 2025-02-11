/** 
 * Enumeration of available field types in the content type system.
 * @enum {string}
 */
export enum TYPES {
  Text = "text",
  SiteOrFolder = "siteOrFolder",
}

/** 
 * Union type of all possible field types.
 * @typedef {TYPES} Fields
 */
export type Fields = `${TYPES}`;

/** 
 * Interface representing a text field configuration.
 * @interface
 */
export interface TextField {
  title: string;
  fieldType: `${TYPES.Text}`;
  required?: boolean;
  hintText?: string;
}

/** 
 * Interface representing a site or host field configuration.
 * @interface
 */
export interface SiteorHostField {
  title: string;
  fieldType: `${TYPES.SiteOrFolder}`;
  required?: boolean;
  hintText?: string;
}

/** 
 * Union type of all possible field type configurations.
 * @typedef {TextField | SiteorHostField} FieldsTypes
 */
export type FieldsTypes = TextField | SiteorHostField;
