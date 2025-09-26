import { createHostFactory, SpectatorHost } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotKeyValueComponent } from '@dotcms/ui';
import {
    createFakeKeyValueField,
    createFakeContentlet,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotKeyValueFieldComponent } from './components/key-value-field/key-value-field.component';
import { DotEditContentKeyValueComponent } from './dot-edit-content-key-value.component';

const KEY_VALUE_FIELD_MOCK = createFakeKeyValueField({
    variable: 'keyValueField'
});

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

describe('DotEditContentKeyValueComponent', () => {
    let spectator: SpectatorHost<DotEditContentKeyValueComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentKeyValueComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [DotKeyValueComponent],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    describe('should initialize correctly', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-key-value [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [KEY_VALUE_FIELD_MOCK.variable]: new FormControl({
                                key1: 'value1',
                                key2: 'value2'
                            })
                        }),
                        field: KEY_VALUE_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [KEY_VALUE_FIELD_MOCK.variable]: {
                                key1: 'value1',
                                key2: 'value2'
                            }
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should set the correct initial value', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            expect(keyValueField.$initialValue()).toEqual([
                { key: 'key1', value: 'value1' },
                { key: 'key2', value: 'value2' }
            ]);
        });

        it('should have the field input set correctly', () => {
            expect(spectator.component.$field()).toBe(KEY_VALUE_FIELD_MOCK);
        });

        it('should render the DotKeyValueComponent', () => {
            const dotKeyValueComponent = spectator.query(DotKeyValueComponent);
            expect(dotKeyValueComponent).toBeTruthy();
        });
    });

    describe('should handle form value updates', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-key-value [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [KEY_VALUE_FIELD_MOCK.variable]: new FormControl({
                                key1: 'value1',
                                key2: 'value2'
                            })
                        }),
                        field: KEY_VALUE_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [KEY_VALUE_FIELD_MOCK.variable]: {
                                key1: 'value1',
                                key2: 'value2'
                            }
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should update form value when DotKeyValueComponent emits updatedList', (done) => {
            const control = spectator.hostComponent.formGroup.get(KEY_VALUE_FIELD_MOCK.variable);

            control.valueChanges.subscribe((value) => {
                expect(value).toEqual({ key14: 'value14' });
                done();
            });

            const dotKeyValue = spectator.query(DotKeyValueComponent);
            dotKeyValue.updatedList.emit([{ key: 'key14', hidden: false, value: 'value14' }]);
            expect(control.touched).toBeTruthy();
        });

        it('should call updateField method when DotKeyValueComponent emits updatedList', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            const updateFieldSpy = jest.spyOn(keyValueField, 'updateField');
            spectator.triggerEventHandler(DotKeyValueComponent, 'updatedList', [
                { key: 'testKey', hidden: false, value: 'testValue' }
            ]);

            const dotKeyValue = spectator.query(DotKeyValueComponent);
            const testData = [{ key: 'testKey', hidden: false, value: 'testValue' }];

            dotKeyValue.updatedList.emit(testData);

            expect(updateFieldSpy).toHaveBeenCalledWith(testData);
        });
    });

    describe('should handle writeValue correctly', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-key-value [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [KEY_VALUE_FIELD_MOCK.variable]: new FormControl({})
                        }),
                        field: KEY_VALUE_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [KEY_VALUE_FIELD_MOCK.variable]: {}
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should parse empty object correctly', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            keyValueField.writeValue({});
            spectator.detectChanges();
            expect(keyValueField.$initialValue()).toEqual([]);
        });

        it('should parse null value correctly', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            keyValueField.writeValue(null);
            spectator.detectChanges();
            expect(keyValueField.$initialValue()).toEqual([]);
        });

        it('should parse undefined value correctly', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            keyValueField.writeValue(undefined);
            spectator.detectChanges();
            expect(keyValueField.$initialValue()).toEqual([]);
        });

        it('should parse valid key-value object correctly', () => {
            const testData = { key1: 'value1', key2: 'value2', key3: 'value3' };
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            keyValueField.writeValue(testData);
            spectator.detectChanges();

            expect(keyValueField.$initialValue()).toEqual([
                { key: 'key1', value: 'value1' },
                { key: 'key2', value: 'value2' },
                { key: 'key3', value: 'value3' }
            ]);
        });
    });

    describe('should handle updateField method', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-key-value [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [KEY_VALUE_FIELD_MOCK.variable]: new FormControl({})
                        }),
                        field: KEY_VALUE_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [KEY_VALUE_FIELD_MOCK.variable]: {}
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should convert DotKeyValue array to object and call onChange', () => {
            // Mock the callbacks
            const mockOnChange = jest.fn();
            const mockOnTouched = jest.fn();

            // Register the mock callbacks
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            keyValueField.registerOnChange(mockOnChange);
            keyValueField.registerOnTouched(mockOnTouched);

            const testData = [
                { key: 'key1', value: 'value1' },
                { key: 'key2', value: 'value2' }
            ];

            keyValueField.updateField(testData);

            expect(mockOnChange).toHaveBeenCalledWith({ key1: 'value1', key2: 'value2' });
            expect(mockOnTouched).toHaveBeenCalled();
        });

        it('should handle empty array correctly', () => {
            // Mock the callbacks
            const mockOnChange = jest.fn();
            const mockOnTouched = jest.fn();

            // Register the mock callbacks
            const keyValueField = spectator.query(DotKeyValueFieldComponent);
            keyValueField.registerOnChange(mockOnChange);
            keyValueField.registerOnTouched(mockOnTouched);

            keyValueField.updateField([]);

            expect(mockOnChange).toHaveBeenCalledWith({});
            expect(mockOnTouched).toHaveBeenCalled();
        });
    });
});
