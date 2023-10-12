import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';
import { FIELD_MOCK } from '../dot-edit-content-field/dot-edit-content-field.component.spec';

export const LAYOUT_MOCK: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Row',
            fieldTypeLabel: 'Row',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051073000,
            id: 'a31ea895f80eb0a3754e4a2292e09a52',
            indexed: false,
            listed: false,
            modDate: 1697051077000,
            name: 'fields-0',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 0,
            unique: false,
            variable: 'fields0'
        },
        columns: [
            {
                columnDivider: {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                    contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                    dataType: 'SYSTEM',
                    fieldType: 'Column',
                    fieldTypeLabel: 'Column',
                    fieldVariables: [],
                    fixed: false,
                    iDate: 1697051073000,
                    id: 'd4c32b4b9fb5b11c58c245d4a02bef47',
                    indexed: false,
                    listed: false,
                    modDate: 1697051077000,
                    name: 'fields-1',
                    readOnly: false,
                    required: false,
                    searchable: false,
                    sortOrder: 1,
                    unique: false,
                    variable: 'fields1'
                },
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                        dataType: 'TEXT',
                        defaultValue: 'Placeholder',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text',
                        fieldVariables: [],
                        fixed: false,
                        hint: 'A hint Text',
                        iDate: 1697051093000,
                        id: '1d1505a4569681b923769acb785fd093',
                        indexed: false,
                        listed: false,
                        modDate: 1697051093000,
                        name: 'name1',
                        readOnly: false,
                        required: true,
                        searchable: false,
                        sortOrder: 2,
                        unique: false,
                        variable: 'name1'
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text',
                        fieldVariables: [],
                        fixed: false,
                        iDate: 1697051107000,
                        id: 'fc776c45044f2d043f5e98eaae36c9ff',
                        indexed: false,
                        listed: false,
                        modDate: 1697051107000,
                        name: 'text2',
                        readOnly: false,
                        required: true,
                        searchable: false,
                        sortOrder: 3,
                        unique: false,
                        variable: 'text2'
                    }
                ]
            },
            {
                columnDivider: {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                    contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                    dataType: 'SYSTEM',
                    fieldType: 'Column',
                    fieldTypeLabel: 'Column',
                    fieldVariables: [],
                    fixed: false,
                    iDate: 1697051077000,
                    id: '848fc78a11e7290efad66eb39333ae2b',
                    indexed: false,
                    listed: false,
                    modDate: 1697051107000,
                    name: 'fields-2',
                    readOnly: false,
                    required: false,
                    searchable: false,
                    sortOrder: 4,
                    unique: false,
                    variable: 'fields2'
                },
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text',
                        fieldVariables: [],
                        fixed: false,
                        hint: 'A hint text2',
                        iDate: 1697051118000,
                        id: '1f6765de8d4ad069ff308bfca56b9255',
                        indexed: false,
                        listed: false,
                        modDate: 1697051118000,
                        name: 'text3',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        sortOrder: 5,
                        unique: false,
                        variable: 'text3'
                    }
                ]
            }
        ]
    }
];

describe('DotFormComponent', () => {
    let spectator: Spectator<DotEditContentFormComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFormComponent,
        imports: [
            DotEditContentFieldComponent,
            CommonModule,
            ReactiveFormsModule,
            ButtonModule,
            DotMessagePipe
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Save: 'Save'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                formData: LAYOUT_MOCK
            }
        });
    });

    // ..
    describe('initilizeForm', () => {
        it('should initialize the form group with form controls for each field in the `formData` array', () => {
            const component = spectator.component;
            component.formData = LAYOUT_MOCK;
            component.initilizeForm();

            expect(component.form.controls['name1']).toBeDefined();
            expect(component.form.controls['text2']).toBeDefined();
        });
    });

    describe('initializeFormControl', () => {
        it('should initialize a form control for a given DotCMSContentTypeField', () => {
            const formControl = spectator.component.initializeFormControl(FIELD_MOCK);

            expect(formControl).toBeDefined();
            expect(formControl.validator).toBeDefined();
            // expect(formControl.validator?.({ value: '123' })).toBeNull();
            // expect(formControl.validator?({ value: 'abc' })).not.toBeNull();
        });
    });

    describe('saveContent', () => {
        it('should emit the form value through the `formSubmit` event', () => {
            const component = spectator.component;
            component.formData = LAYOUT_MOCK;
            component.initilizeForm();

            jest.spyOn(component.formSubmit, 'emit');

            component.saveContenlet();

            expect(component.formSubmit.emit).toHaveBeenCalledWith(component.form.value);
        });
    });
});
