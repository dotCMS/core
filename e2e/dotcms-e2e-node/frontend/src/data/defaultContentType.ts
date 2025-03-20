import { FieldsTypes } from "@models/newContentType.model";

export function createDefaultContentType() {
  const defaultTypes: FieldsTypes[] = [
    {
      title: "Text Field",
      fieldType: "text",
    },
    {
      title: "Site or Folder Field",
      fieldType: "siteOrFolder",
    },
  ];
  return defaultTypes;
}
