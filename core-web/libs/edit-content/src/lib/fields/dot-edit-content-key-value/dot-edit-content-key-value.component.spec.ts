import { createHostFactory, SpectatorHost } from '@openng/spectator';
import { vi } from 'vitest';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';

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
    formGroup!: FormGroup;
    field!: DotCMSContentTypeField;
    contentlet!: DotCMSContentlet;
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
            ConfirmationService,
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
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
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

        it('should update form value when DotKeyValueComponent emits updatedList', () =>
            new Promise<void>((done) => {
                const control = spectator.hostComponent.formGroup.get(
                    KEY_VALUE_FIELD_MOCK.variable
                );

                control!.valueChanges.subscribe((value) => {
                    // JSON text, not an object, so key order survives.
                    expect(JSON.parse(value)).toEqual({ key14: 'value14' });
                    done();
                });

                const dotKeyValue = spectator.query(DotKeyValueComponent)!;
                dotKeyValue!.updatedList.emit([{ key: 'key14', hidden: false, value: 'value14' }]);
                expect(control!.touched).toBeTruthy();
            }));

        it('should call updateField method when DotKeyValueComponent emits updatedList', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            const updateFieldSpy = vi.spyOn(keyValueField!, 'updateField');
            spectator.triggerEventHandler(DotKeyValueComponent, 'updatedList', [
                { key: 'testKey', hidden: false, value: 'testValue' }
            ]);

            const dotKeyValue = spectator.query(DotKeyValueComponent)!;
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
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.writeValue({});
            spectator.detectChanges();
            expect(keyValueField.$initialValue()).toEqual([]);
        });

        it('should parse null value correctly', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.writeValue(null);
            spectator.detectChanges();
            expect(keyValueField.$initialValue()).toEqual([]);
        });

        it('should parse undefined value correctly', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            // `undefined` is not part of the declared contract — Angular clears a control with
            // `null` — but this test deliberately checks the accessor tolerates it.
            keyValueField.writeValue(undefined as unknown as Record<string, string | null>);
            spectator.detectChanges();
            expect(keyValueField.$initialValue()).toEqual([]);
        });

        it('should display null values as the string "null" after import', () => {
            const testData = { key1: null, key2: 'value2' };
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.writeValue(testData);
            spectator.detectChanges();

            expect(keyValueField.$initialValue()).toEqual([
                { key: 'key1', value: 'null' },
                { key: 'key2', value: 'value2' }
            ]);
        });

        it('should parse valid key-value object correctly', () => {
            const testData = { key1: 'value1', key2: 'value2', key3: 'value3' };
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.writeValue(testData);
            spectator.detectChanges();

            expect(keyValueField.$initialValue()).toEqual([
                { key: 'key1', value: 'value1' },
                { key: 'key2', value: 'value2' },
                { key: 'key3', value: 'value3' }
            ]);
        });

        it('should not split string values into individual characters', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.writeValue('[object Object]' as unknown as Record<string, string | null>);
            spectator.detectChanges();

            expect(keyValueField.$initialValue()).toEqual([]);
        });

        it('should not split array values into individual entries', () => {
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.writeValue(['key1', 'key2'] as unknown as Record<string, string | null>);
            spectator.detectChanges();

            expect(keyValueField.$initialValue()).toEqual([]);
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

        it('should report the pairs as ordered JSON and call onChange', () => {
            // Mock the callbacks
            const mockOnChange = vi.fn();
            const mockOnTouched = vi.fn();

            // Register the mock callbacks
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.registerOnChange(mockOnChange);
            keyValueField.registerOnTouched(mockOnTouched);

            const testData = [
                { key: 'key1', value: 'value1' },
                { key: 'key2', value: 'value2' }
            ];

            keyValueField.updateField(testData);

            expect(mockOnChange).toHaveBeenCalledWith('{"key1":"value1","key2":"value2"}');
            expect(mockOnTouched).toHaveBeenCalled();
        });

        it('should handle empty array correctly', () => {
            // Mock the callbacks
            const mockOnChange = vi.fn();
            const mockOnTouched = vi.fn();

            // Register the mock callbacks
            const keyValueField = spectator.query(DotKeyValueFieldComponent)!;
            keyValueField.registerOnChange(mockOnChange);
            keyValueField.registerOnTouched(mockOnTouched);

            keyValueField.updateField([]);

            expect(JSON.parse(mockOnChange.mock.calls[0][0])).toEqual({});
            expect(mockOnTouched).toHaveBeenCalled();
        });
    });
});

/**
 * AC-209 — naming a widget whose value is a collection.
 *
 * Key/Value has no single control either: the visible inputs are the two boxes used to ADD a pair,
 * while the field's value is the list they build. So the widget as a whole becomes the named thing,
 * exactly as a checkbox set does.
 *
 * No aria-required, for the same reason as the checkbox group: ARIA defines that attribute on
 * radiogroup, not on a plain `group`.
 */
describe('DotEditContentKeyValueComponent — group semantics (AC-209)', () => {
    let spectator: SpectatorHost<DotEditContentKeyValueComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentKeyValueComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [DotKeyValueComponent],
        providers: [
            ConfirmationService,
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    const render = (required: boolean) => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-key-value [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [KEY_VALUE_FIELD_MOCK.variable]: new FormControl({})
                    }),
                    field: { ...KEY_VALUE_FIELD_MOCK, required },
                    contentlet: createFakeContentlet({ [KEY_VALUE_FIELD_MOCK.variable]: {} })
                }
            }
        );
        spectator.detectChanges();

        // Throwing rather than returning `T | null` keeps every caller's assertion honest: with
        // an optional chain, the `aria-required` assertion below would pass just as happily if the
        // widget never rendered at all.
        const widget = spectator.query('dot-key-value-field');
        if (!widget) {
            throw new Error('the key-value widget did not render');
        }

        return widget;
    };

    it('should expose the widget as a group', () => {
        expect(render(true).getAttribute('role')).toBe('group');
    });

    it('should name the group from the field label', () => {
        const widget = render(true);

        expect(widget.getAttribute('aria-labelledby')).toBe(
            'label-' + KEY_VALUE_FIELD_MOCK.variable
        );
        expect(spectator.query('label[dotCardFieldLabel]')?.id).toBe(
            'label-' + KEY_VALUE_FIELD_MOCK.variable
        );
    });

    it('should not put aria-required on a plain group, which ARIA does not define it on', () => {
        expect(render(true).getAttribute('aria-required')).toBeNull();
    });
});
