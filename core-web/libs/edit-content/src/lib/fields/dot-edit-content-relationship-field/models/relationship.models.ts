import { MANDATORY_FIELDS } from '../dot-edit-content-relationship-field.constants';

export type MandatoryFields = typeof MANDATORY_FIELDS;

export interface RelationshipFieldItem extends MandatoryFields {
    id: string;
    [key: string]: string;
}
