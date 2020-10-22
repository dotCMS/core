import { Observable, of } from 'rxjs';
import * as _ from 'lodash';
import { DotFieldVariable } from '@portlets/shared/dot-content-types-edit/components/fields/dot-content-type-fields-variables/models/dot-field-variable.interface';

export const mockFieldVariables: DotFieldVariable[] = [
    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
        fieldId: 'f965a51b-130a-435f-b646-41e07d685363',
        id: '9671d2c3-793b-41af-a485-e2c5fcba5fb',
        key: 'Key1',
        value: 'Value1'
    },
    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
        fieldId: 'f965a51b-130a-435f-b646-41e07d685364',
        id: '9671d2c3-793b-41af-a485-e2c5fcba5fa',
        key: 'Key2',
        value: 'Value2'
    },
    {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
        fieldId: 'f965a51b-130a-435f-b646-41e07d685365',
        id: '9671d2c3-793b-41af-a485-e2c5fcba5fc',
        key: 'Key3',
        value: 'Value3'
    }
];

export class DotFieldVariablesServiceMock {
    load(): Observable<DotFieldVariable[]> {
        return of(_.cloneDeep(mockFieldVariables));
    }

    save(): Observable<DotFieldVariable> {
        return of(_.cloneDeep(mockFieldVariables[0]));
    }

    delete(): Observable<string> {
        return of('');
    }
}
