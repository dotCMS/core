import { Observable, of } from 'rxjs';

import { DotFieldVariable } from '@dotcms/dotcms-models';

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
        return of(structuredClone(mockFieldVariables));
    }

    save(): Observable<DotFieldVariable> {
        return of(structuredClone(mockFieldVariables[0]));
    }

    delete(): Observable<string> {
        return of('');
    }
}
