import {
    createDirectiveFactory,
    mockProvider,
    SpectatorDirective,
    SpyObject
} from '@openng/spectator/jest';

import { Component, ViewContainerRef } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { DotCMSClazzes, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DynamicFieldPropertyDirective } from './dynamic-field-property.directive';

import { FieldPropertyService } from '../../../service';
import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-custom-host',
    template: '',
    standalone: false
})
class CustomHostComponent {
    propertyName = 'name';
    group = new UntypedFormGroup({});
    field: DotCMSContentTypeField = {
        name: 'FieldName',
        clazz: DotCMSClazzes.TEXT,
        contentTypeId: '123',
        dataType: 'text',
        fieldType: 'text',
        fieldTypeLabel: 'Text',
        fieldVariables: [],
        fixed: false,
        iDate: 123,
        id: '123',
        indexed: false,
        listed: false,
        modDate: 123,
        required: false,
        searchable: false,
        sortOrder: 1,
        unique: false,
        variable: 'name',
        readOnly: false
    };
}

@Component({
    selector: 'dot-test',
    template: '<p>Dynamic Component</p>',
    standalone: false
})
class DynamicComponent {
    property: FieldProperty;
    group: UntypedFormGroup;
    helpText: string;
}

describe('Directive: DynamicFieldPropertyDirective', () => {
    let hostSpectator: SpectatorDirective<DynamicFieldPropertyDirective, CustomHostComponent>;
    let fieldPropertyService: SpyObject<FieldPropertyService>;

    const createDirective = createDirectiveFactory({
        directive: DynamicFieldPropertyDirective,
        host: CustomHostComponent,
        providers: [
            mockProvider(FieldPropertyService, {
                getComponent: jest.fn().mockReturnValue(DynamicComponent),
                getFieldType: jest.fn().mockReturnValue({
                    helpText: 'helpText'
                })
            }),
            mockProvider(ViewContainerRef)
        ],
        detectChanges: false
    });

    beforeEach(() => {
        hostSpectator = createDirective(`<ng-container
            [propertyName]="propertyName"
            [field]="field"
            [group]="group"
            dotDynamicFieldProperty />`);

        fieldPropertyService = hostSpectator.inject(FieldPropertyService);
    });

    it('Should set component properties', () => {
        hostSpectator.detectChanges();
        expect(fieldPropertyService.getComponent).toHaveBeenCalledWith('name');
        expect(fieldPropertyService.getComponent).toHaveBeenCalledTimes(1);

        expect(hostSpectator.query('dot-test')).toContainText('Dynamic Component');

        const testComponent = hostSpectator.query(DynamicComponent);
        expect(testComponent).toBeDefined();
        expect(testComponent.property).toEqual({
            field: hostSpectator.hostComponent.field,
            name: hostSpectator.hostComponent.propertyName,
            value: hostSpectator.hostComponent.field.name
        });
        expect(testComponent.group).toEqual(hostSpectator.hostComponent.group);
        expect(testComponent.helpText).toEqual('helpText');
    });

    describe('shouldRecreate — field reference identity', () => {
        const buildNewField = (overrides: Partial<DotCMSContentTypeField> = {}) =>
            ({
                ...hostSpectator.hostComponent.field,
                id: null,
                ...overrides
            }) as DotCMSContentTypeField;

        beforeEach(() => {
            fieldPropertyService.getComponent.mockClear();
        });

        it('recreates the component when switching between two new fields with null id but different references', () => {
            hostSpectator.hostComponent.field = buildNewField({ variable: 'first' });
            hostSpectator.detectChanges();
            const callsAfterFirst = fieldPropertyService.getComponent.mock.calls.length;

            hostSpectator.hostComponent.field = buildNewField({ variable: 'second' });
            hostSpectator.detectChanges();

            expect(fieldPropertyService.getComponent.mock.calls.length).toBe(callsAfterFirst + 1);
        });

        it('does not recreate the component when the same field reference is passed again', () => {
            const sameField = buildNewField({ variable: 'stable' });
            hostSpectator.hostComponent.field = sameField;
            hostSpectator.detectChanges();
            const callsAfterFirst = fieldPropertyService.getComponent.mock.calls.length;

            hostSpectator.hostComponent.field = sameField;
            hostSpectator.detectChanges();

            expect(fieldPropertyService.getComponent.mock.calls.length).toBe(callsAfterFirst);
        });

        it('recreates the component when switching from a saved field to a new null-id field', () => {
            hostSpectator.detectChanges();
            const callsAfterSaved = fieldPropertyService.getComponent.mock.calls.length;

            hostSpectator.hostComponent.field = buildNewField({ variable: 'brand-new' });
            hostSpectator.detectChanges();

            expect(fieldPropertyService.getComponent.mock.calls.length).toBe(callsAfterSaved + 1);
        });
    });
});
