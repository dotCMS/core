import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotRenderModes, NEW_RENDER_MODE_VARIABLE_KEY } from '@dotcms/dotcms-models';
import { WINDOW } from '@dotcms/utils';
import { createFakeContentlet, createFakeCustomField } from '@dotcms/utils-testing';

import { IframeFieldComponent } from './components/iframe-field/iframe-field.component';
import { NativeFieldComponent } from './components/native-field/native-field.component';
import { DotEditContentCustomFieldComponent } from './dot-edit-content-custom-field.component';

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
});
