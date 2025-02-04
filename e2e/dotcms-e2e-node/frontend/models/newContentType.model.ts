export enum TYPES {
  Text = "text",
  SiteOrFolder = "siteOrFolder",
}

export type Fields = `${TYPES}`;

export interface TextField {
  title: string;
  fieldType: `${TYPES.Text}`;
  requeried?: boolean;
  hintText?: string;
}

export interface SiteorHostField {
  title: string;
  fieldType: `${TYPES.SiteOrFolder}`;
  requeried?: boolean;
  hintText?: string;
}

export type FieldsTypes = TextField | SiteorHostField;
