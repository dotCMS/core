import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { WINDOW } from '@dotcms/utils';
import { createFakeContentlet, createFakeCustomField } from '@dotcms/utils-testing';

import { IframeFieldComponent } from './iframe-field.component';

const MOCK_CONTENT_TYPE_NAME = 'test';
const MOCK_INODE = 'test-inode';

const CUSTOM_FIELD_OPTIONS = {
    key: 'customFieldOptions',
    value: '{"showAsModal": true,"width": "398px", "height": "650px"}',
    id: 'customFieldOptions',
    fieldId: '123',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
};

const CUSTOM_FIELD_OPTIONS_NO_MODAL = {
    key: 'customFieldOptions',
    value: '{"width": "398px", "height": "650px"}',
    id: 'customFieldOptions',
    fieldId: '123',
    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
};

describe('IframeFieldComponent', () => {
    let spectator: SpectatorHost<IframeFieldComponent>;

    const CUSTOM_FIELD_WITH_VARIABLES = createFakeCustomField({
        fieldVariables: [CUSTOM_FIELD_OPTIONS]
    });

    const CUSTOM_FIELD_WITHOUT_MODAL = createFakeCustomField({
        fieldVariables: [CUSTOM_FIELD_OPTIONS_NO_MODAL]
    });

    const createHost = createHostFactory({
        component: IframeFieldComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
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
                    <dot-iframe-field
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

        it('should initialize with default values', () => {
            expect(spectator.component.$isFullscreen()).toBe(false);
            expect(spectator.component.$variables()).toEqual({});
        });
    });

    describe('Iframe Rendering', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [CUSTOM_FIELD_WITHOUT_MODAL.variable]: new FormControl('')
                        }),
                        field: CUSTOM_FIELD_WITHOUT_MODAL,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [CUSTOM_FIELD_WITHOUT_MODAL.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should render iframe with correct attributes', async () => {
            await spectator.fixture.whenStable();
            const iframe = spectator.query(byTestId('custom-field-iframe'));
            expect(spectator.component.$src()).not.toBe('');
            expect(iframe).toBeTruthy();
            expect(iframe?.getAttribute('src')).toContain('legacy-custom-field.jsp');
            expect(iframe?.getAttribute('title')).toContain('Content Type');
            expect(iframe?.classList.contains('legacy-custom-field__iframe')).toBe(true);
        });

        it('should not render iframe when src is empty', () => {
            spectator.setHostInput('field', null);
            spectator.detectChanges();

            const iframe = spectator.query(byTestId('custom-field-iframe'));
            expect(iframe).toBeFalsy();
        });
    });

    describe('URL Generation', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
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

        it('should compute correct src url with all parameters', () => {
            expect(spectator.component.$src()).toContain('legacy-custom-field.jsp');
            expect(spectator.component.$src()).toContain(`variable=${MOCK_CONTENT_TYPE_NAME}`);
            expect(spectator.component.$src()).toContain(
                `field=${CUSTOM_FIELD_WITH_VARIABLES.variable}`
            );
            expect(spectator.component.$src()).toContain(`inode=${MOCK_INODE}`);
            expect(spectator.component.$src()).toContain('modal=true');
        });

        it('should return empty string when field is null', () => {
            spectator.setHostInput('field', null);
            spectator.detectChanges();

            expect(spectator.component.$src()).toBe('');
        });
    });

    describe('URL Generation with null contentType', () => {
        it('should return empty string when contentType is null', () => {
            const field = createFakeCustomField({
                fieldVariables: [CUSTOM_FIELD_OPTIONS]
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
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
                        contentTypeVariable: null
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$src()).toBe('');
        });

        it('should include modal parameter when showAsModal is true', () => {
            const field = createFakeCustomField({
                fieldVariables: [CUSTOM_FIELD_OPTIONS]
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
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

            expect(spectator.component.$src()).toContain('modal=true');
        });
    });

    describe('Fullscreen Functionality', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
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

        it('should show close button only in fullscreen mode', async () => {
            await spectator.fixture.whenStable();
            expect(spectator.query('p-button[icon="pi pi-times"]')).toBeFalsy();

            spectator.component.$isFullscreen.set(true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(spectator.query('p-button[icon="pi pi-times"]')).toBeTruthy();
        });

        it('should exit fullscreen when close button is clicked', async () => {
            await spectator.fixture.whenStable();
            spectator.component.$isFullscreen.set(true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const closeButton = spectator.query('p-button[icon="pi pi-times"]');
            spectator.click(closeButton);

            expect(spectator.component.$isFullscreen()).toBe(false);
        });
    });

    describe('Message Handling', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
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

        it('should handle window message for toggling fullscreen', () => {
            const initialState = spectator.component.$isFullscreen();
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: { type: 'toggleFullscreen' },
                    origin: window.location.origin
                })
            );
            expect(spectator.component.$isFullscreen()).toBe(!initialState);
        });

        it('should ignore messages from unauthorized origins', () => {
            const initialState = spectator.component.$isFullscreen();
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: { type: 'toggleFullscreen' },
                    origin: 'https://unauthorized.com'
                })
            );
            expect(spectator.component.$isFullscreen()).toBe(initialState);
        });

        it('should ignore messages with unknown types', () => {
            const initialState = spectator.component.$isFullscreen();
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: { type: 'unknownType' },
                    origin: window.location.origin
                })
            );
            expect(spectator.component.$isFullscreen()).toBe(initialState);
        });

        it('should handle iframe resize messages', () => {
            const iframe = spectator.query(byTestId('custom-field-iframe')) as HTMLIFrameElement;
            if (!iframe) return;

            const initialHeight = iframe.style.height;
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: { type: 'dotcms:iframe:resize', height: 500 },
                    origin: window.location.origin
                })
            );

            expect(iframe.style.height).toBe('500px');
            expect(iframe.style.height).not.toBe(initialHeight);
        });

        it('should ignore resize messages when showAsModal is true', () => {
            const fieldWithModal = {
                ...CUSTOM_FIELD_WITH_VARIABLES,
                fieldVariables: [
                    ...CUSTOM_FIELD_WITH_VARIABLES.fieldVariables,
                    {
                        key: 'showAsModal',
                        value: 'true',
                        id: 'showAsModal',
                        fieldId: '123',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                    }
                ]
            };

            spectator.setHostInput('field', fieldWithModal);
            spectator.detectChanges();

            const iframe = spectator.query(
                byTestId('custom-field-modal-iframe')
            ) as HTMLIFrameElement;
            if (!iframe) return;

            const initialHeight = iframe.style.height;
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: { type: 'dotcms:iframe:resize', height: 500 },
                    origin: window.location.origin
                })
            );

            // Height should not change in modal mode
            expect(iframe.style.height).toBe(initialHeight);
        });
    });

    describe('Form Bridge Integration', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
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

        it('should send form loaded message on iframe load', async () => {
            await spectator.fixture.whenStable();
            // In modal mode, we need to open the modal first to get the iframe
            spectator.component.$showModal.set(true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            spectator.flushEffects();
            spectator.detectChanges();
            await new Promise((resolve) => setTimeout(resolve, 200));

            const iframe = spectator.query(
                byTestId('custom-field-modal-iframe')
            ) as HTMLIFrameElement;
            if (!iframe || !iframe.contentWindow) return;

            const postMessageSpy = jest.spyOn(iframe.contentWindow, 'postMessage');
            spectator.component.onIframeLoad();

            expect(postMessageSpy).toHaveBeenCalledWith(
                { type: 'dotcms:form:loaded' },
                window.location.origin
            );
        });

        it('should initialize variables on iframe load', async () => {
            await spectator.fixture.whenStable();
            // In modal mode, we need to open the modal first to get the iframe
            spectator.component.$showModal.set(true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            spectator.flushEffects();
            spectator.detectChanges();
            await new Promise((resolve) => setTimeout(resolve, 200));

            // Now we can call onIframeLoad since the iframe exists in the modal
            spectator.component.onIframeLoad();

            expect(spectator.component.$variables()).toEqual({
                customFieldOptions: '{"showAsModal": true,"width": "398px", "height": "650px"}'
            });
        });
    });

    describe('Modal Mode', () => {
        const fieldWithModal = {
            ...CUSTOM_FIELD_WITH_VARIABLES,
            fieldVariables: [
                ...CUSTOM_FIELD_WITH_VARIABLES.fieldVariables,
                {
                    key: 'showAsModal',
                    value: 'true',
                    id: 'showAsModal',
                    fieldId: '123',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
                }
            ]
        };

        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-iframe-field
                        [field]="field"
                        [contentlet]="contentlet"
                        [contentType]="contentTypeVariable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithModal.variable]: new FormControl('')
                        }),
                        field: fieldWithModal,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithModal.variable]: ''
                        }),
                        contentTypeVariable: MOCK_CONTENT_TYPE_NAME
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should render input and button instead of iframe', async () => {
            await spectator.fixture.whenStable();
            const input = spectator.query(`[data-testId="${fieldWithModal.variable}"]`);
            const button = spectator.query(byTestId('custom-field-show-button'));
            const iframe = spectator.query(byTestId('custom-field-iframe'));

            expect(input).toBeTruthy();
            expect(button).toBeTruthy();
            expect(iframe).toBeFalsy();
        });

        it('should open modal when button is clicked', async () => {
            await spectator.fixture.whenStable();
            const button = spectator.query(byTestId('custom-field-show-button'));
            expect(spectator.component.$showModal()).toBe(false);

            spectator.click(button);
            spectator.detectChanges();

            expect(spectator.component.$showModal()).toBe(true);
        });

        it('should render iframe inside modal when modal is open', async () => {
            await spectator.fixture.whenStable();
            expect(spectator.component.$showModal()).toBe(false);

            spectator.component.$showModal.set(true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            // Flush effects to ensure defer executes
            spectator.flushEffects();
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            // Verify modal is open
            expect(spectator.component.$showModal()).toBe(true);

            // Wait for defer to load and dialog to render
            await new Promise((resolve) => setTimeout(resolve, 300));

            // Check if modal dialog exists first
            const modalDialog = spectator.query(byTestId('custom-field-modal'));
            expect(modalDialog).toBeTruthy();

            // Then check for iframe inside modal
            const modalIframe = spectator.query(byTestId('custom-field-modal-iframe'));
            expect(modalIframe).toBeTruthy();
        });

        it('should close modal when done button is clicked', async () => {
            await spectator.fixture.whenStable();
            spectator.component.$showModal.set(true);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            // Wait for defer to load
            await new Promise((resolve) => setTimeout(resolve, 100));

            const doneButton = spectator.query('p-button[label="Done"]');
            if (doneButton) {
                spectator.click(doneButton);
                spectator.detectChanges();

                expect(spectator.component.$showModal()).toBe(false);
            }
        });
    });
});
