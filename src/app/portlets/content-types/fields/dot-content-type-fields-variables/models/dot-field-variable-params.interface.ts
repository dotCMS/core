import { DotFieldVariable } from './dot-field-variable.interface';

export interface DotFieldVariableParams {
    contentTypeId: string;
    fieldId: string;
    variable?: DotFieldVariable;
}
