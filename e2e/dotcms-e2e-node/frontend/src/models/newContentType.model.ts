/**
 * Enumeration of available field types in the content type system.
 * @enum {string}
 */
export enum TYPES {
  Text = "text",
  SiteOrFolder = "siteOrFolder",
  Relationship = "relationship",
}

/**
 * Union type of all possible field types.
 * @typedef {TYPES} Fields
 */
export type Fields = `${TYPES}`;

export interface GenericField {
  title: string;
  fieldType: Fields;
  required?: boolean;
  hintText?: string;
}

/**
 * Interface representing a text field configuration.
 * @interface
 */
export interface TextField extends GenericField {
  fieldType: `${TYPES.Text}`;
}

/**
 * Interface representing a site or host field configuration.
 * @interface
 */
export interface SiteorHostField extends GenericField {
  fieldType: `${TYPES.SiteOrFolder}`;
}

/**
 * Interface representing a relationship field configuration.
 * @interface
 */
export interface RelationshipField extends GenericField {
  fieldType: `${TYPES.Relationship}`;
  entityToRelate: string;
  cardinality: "1-1" | "1-many" | "many-1" | "many-many";
}

/**
 * Union type of all possible field type configurations.
 * @typedef {TextField | SiteorHostField | RelationshipField} FieldsTypes
 */
export type FieldsTypes = TextField | SiteorHostField | RelationshipField;
