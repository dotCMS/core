import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DotRenderModes, NEW_RENDER_MODE_VARIABLE_KEY } from '@dotcms/dotcms-models';
import { DotMessagePipe as RealDotMessagePipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import { createFakeContentlet, createFakeCustomField, DotMessagePipe } from '@dotcms/utils-testing';

import { IframeFieldComponent } from './components/iframe-field/iframe-field.component';
import { NativeFieldComponent } from './components/native-field/native-field.component';
import { DotEditContentCustomFieldComponent } from './dot-edit-content-custom-field.component';

import { DotEditContentStore } from '../../store/edit-content.store';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';

const MOCK_CONTENT_TYPE_NAME = 'test';
const MOCK_INODE = 'test-inode';

describe('DotEditContentCustomFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentCustomFieldComponent>;

    const FIELD_VARIABLES = {
        height: '300px',
        width: '100%'
    };

    const CUSTOM_FIELD_WITH_VARIABLES = createFakeCustomField({
        fieldVariables: Object.entries(FIELD_VARIABLES).map(([key, value]) => ({
            key,
            value,
            id: key,
            fieldId: '123',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
        }))
    });

    const createHost = createHostFactory({
        component: DotEditContentCustomFieldComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [DotCardFieldComponent, DotCardFieldContentComponent],
        providers: [
            {
                provide: WINDOW,
                useValue: window
            },
            {
                provide: DotEditContentStore,
                useValue: {
                    setFieldVisibility: jest.fn()
                }
            }
        ]
    });

    describe('Component Initialization', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [CUSTOM_FIELD_WITH_VARIABLES.variable]: new FormControl('')
                        }),
                        field: CUSTOM_FIELD_WITH_VARIABLES,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [CUSTOM_FIELD_WITH_VARIABLES.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should compute render mode as IFRAME by default', () => {
            expect(spectator.component.$renderMode()).toBe(DotRenderModes.IFRAME);
            expect(spectator.component.$isIframeStrategy()).toBe(true);
        });
    });

    describe('Render Mode Computation', () => {
        it('should return IFRAME when field has no fieldVariables or is null', () => {
            // Note: Template cannot handle null field, but the computed logic does
            // This test verifies the default behavior (no fieldVariables = IFRAME)
            // which is equivalent to the null case handled by the computed
            const fieldWithoutVariables = createFakeCustomField({
                fieldVariables: []
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutVariables.variable]: new FormControl('')
                        }),
                        field: fieldWithoutVariables,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithoutVariables.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$renderMode()).toBe(DotRenderModes.IFRAME);
            expect(spectator.component.$isIframeStrategy()).toBe(true);
        });

        it('should return IFRAME when newRenderMode variable is not set', () => {
            const field = createFakeCustomField();

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.variable]: new FormControl('')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [field.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$renderMode()).toBe(DotRenderModes.IFRAME);
            expect(spectator.component.$isIframeStrategy()).toBe(true);
        });

        it('should return COMPONENT when newRenderMode is set to component', () => {
            const fieldWithComponentMode = createFakeCustomField({
                fieldVariables: [
                    {
                        key: NEW_RENDER_MODE_VARIABLE_KEY,
                        value: DotRenderModes.COMPONENT,
                        id: NEW_RENDER_MODE_VARIABLE_KEY,
                        fieldId: '123',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                    }
                ]
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithComponentMode.variable]: new FormControl('')
                        }),
                        field: fieldWithComponentMode,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithComponentMode.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$renderMode()).toBe(DotRenderModes.COMPONENT);
            expect(spectator.component.$isIframeStrategy()).toBe(false);
        });

        it('should return IFRAME when newRenderMode is set to iframe', () => {
            const fieldWithIframeMode = createFakeCustomField({
                fieldVariables: [
                    {
                        key: NEW_RENDER_MODE_VARIABLE_KEY,
                        value: DotRenderModes.IFRAME,
                        id: NEW_RENDER_MODE_VARIABLE_KEY,
                        fieldId: '123',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                    }
                ]
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithIframeMode.variable]: new FormControl('')
                        }),
                        field: fieldWithIframeMode,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithIframeMode.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$renderMode()).toBe(DotRenderModes.IFRAME);
            expect(spectator.component.$isIframeStrategy()).toBe(true);
        });
    });

    describe('Conditional Rendering', () => {
        it('should render IframeFieldComponent when render mode is IFRAME', () => {
            const field = createFakeCustomField();

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.variable]: new FormControl('')
                        }),
                        field: field,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [field.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();

            const iframeField = spectator.query(IframeFieldComponent);
            const nativeField = spectator.query(NativeFieldComponent);

            expect(iframeField).toBeTruthy();
            expect(nativeField).toBeFalsy();
        });

        it('should render NativeFieldComponent when render mode is COMPONENT', () => {
            const fieldWithComponentMode = createFakeCustomField({
                fieldVariables: [
                    {
                        key: NEW_RENDER_MODE_VARIABLE_KEY,
                        value: DotRenderModes.COMPONENT,
                        id: NEW_RENDER_MODE_VARIABLE_KEY,
                        fieldId: '123',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                    }
                ]
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithComponentMode.variable]: new FormControl('')
                        }),
                        field: fieldWithComponentMode,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithComponentMode.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();

            const iframeField = spectator.query(IframeFieldComponent);
            const nativeField = spectator.query(NativeFieldComponent);

            expect(iframeField).toBeFalsy();
            expect(nativeField).toBeTruthy();
        });
    });

    describe('Required Validation', () => {
        // This block uses a separate factory that does NOT mock DotCardFieldComponent
        // because the inline error message is content-projected through it.
        const createHostNoMocks = createHostFactory({
            component: DotEditContentCustomFieldComponent,
            imports: [ReactiveFormsModule],
            detectChanges: false,
            // Replace the real DotMessagePipe with the test pipe that returns the i18n key as-is.
            overrideComponents: [
                [
                    DotEditContentCustomFieldComponent,
                    {
                        remove: { imports: [RealDotMessagePipe] },
                        add: { imports: [DotMessagePipe] }
                    }
                ]
            ],
            providers: [
                {
                    provide: WINDOW,
                    useValue: window
                },
                {
                    provide: DotEditContentStore,
                    useValue: {
                        setFieldVisibility: jest.fn()
                    }
                }
            ]
        });

        const REQUIRED_FIELD = { ...createFakeCustomField(), required: true };

        const renderRequiredField = (initialValue = '') => {
            const formGroup = new FormGroup({
                [REQUIRED_FIELD.variable]: new FormControl(initialValue, Validators.required)
            });

            spectator = createHostNoMocks(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup,
                        field: REQUIRED_FIELD,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [REQUIRED_FIELD.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();
            spectator.flushEffects();

            return formGroup;
        };

        it('should not render the inline error before the control is touched', () => {
            renderRequiredField();

            expect(spectator.query('small.p-field-error')).toBeNull();
        });

        it('should render the inline error when the required field is empty and touched', () => {
            const formGroup = renderRequiredField();

            const control = formGroup.get(REQUIRED_FIELD.variable);
            control.setErrors({ required: true });
            control.markAsTouched();
            spectator.detectChanges();

            const errorEl = spectator.query('small.p-field-error');
            expect(errorEl).toBeTruthy();
            expect(errorEl.textContent.trim()).toBe('dot.edit.content.form.field.required');
        });

        it('should remove the inline error after the field receives a valid value', () => {
            const formGroup = renderRequiredField();

            const control = formGroup.get(REQUIRED_FIELD.variable);
            control.setErrors({ required: true });
            control.markAsTouched();
            spectator.detectChanges();

            expect(spectator.query('small.p-field-error')).toBeTruthy();

            control.setValue('something');
            control.setErrors(null);
            spectator.detectChanges();

            expect(spectator.query('small.p-field-error')).toBeNull();
        });
    });
});
