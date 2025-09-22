import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { WINDOW } from '@dotcms/utils';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentCustomFieldComponent } from './dot-edit-content-custom-field.component';

import { CUSTOM_FIELD_MOCK } from '../../utils/mocks';
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

    const CUSTOM_FIELD_WITH_VARIABLES = {
        ...CUSTOM_FIELD_MOCK,
        fieldVariables: Object.entries(FIELD_VARIABLES).map(([key, value]) => ({
            key,
            value,
            id: key,
            fieldId: '123',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable'
        }))
    };

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
                        [formControlName]="field.variable"
                        [inode]="contentlet?.inode"
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
            expect(spectator.component.$variables()).toEqual(
                CUSTOM_FIELD_MOCK.fieldVariables.reduce((acc, { key, value }) => {
                    acc[key] = value;

                    return acc;
                }, {})
            );
        });
    });

    describe('Iframe Rendering', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [formControlName]="field.variable"
                        [inode]="contentlet?.inode"
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

        it('should render iframe with correct attributes', () => {
            spectator.fixture.whenStable().then(() => {
                const iframe = spectator.query(byTestId('custom-field-iframe'));
                expect(spectator.component.$src()).not.toBe('');
                expect(iframe).toBeTruthy();
                expect(iframe?.getAttribute('src')).toContain('legacy-custom-field.jsp');
                expect(iframe?.getAttribute('title')).toContain('Content Type');
                expect(iframe?.classList.contains('legacy-custom-field__iframe')).toBe(true);
            });
        });

        it('should apply correct styles when not in fullscreen', () => {
            spectator.fixture.whenStable().then(() => {
                const iframe = spectator.query(byTestId('custom-field-iframe'));
                const variables = spectator.component.$variables();
                expect(iframe).toHaveStyle({
                    height: variables.height,
                    width: variables.width
                });
            });
        });

        it('should apply fullscreen class when in fullscreen mode', () => {
            spectator.fixture.whenStable().then(() => {
                spectator.component.$isFullscreen.set(true);
                spectator.detectChanges();

                const iframe = spectator.query(byTestId('custom-field-iframe'));
                expect(iframe).toHaveClass('legacy-custom-field--fullscreen');
            });
        });
    });

    describe('URL Generation', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [formControlName]="field.variable"
                        [inode]="contentlet?.inode"
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
            spectator.fixture.whenStable().then(() => {
                const expectedUrl = `/html/legacy_custom_field/legacy-custom-field.jsp?variable=${MOCK_CONTENT_TYPE_NAME}&field=${CUSTOM_FIELD_MOCK.variable}&inode=${MOCK_INODE}`;
                expect(spectator.component.$src()).toBe(expectedUrl);
            });
        });
    });

    describe('Fullscreen Functionality', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [formControlName]="field.variable"
                        [inode]="contentlet?.inode"
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

        it('should show close button only in fullscreen mode', () => {
            spectator.fixture.whenStable().then(() => {
                expect(spectator.query('p-button')).toBeFalsy();

                spectator.component.$isFullscreen.set(true);
                spectator.detectChanges();

                expect(spectator.query('p-button')).toBeTruthy();
            });
        });

        it('should exit fullscreen when close button is clicked', () => {
            spectator.fixture.whenStable().then(() => {
                spectator.component.$isFullscreen.set(true);
                spectator.detectChanges();

                const closeButton = spectator.query('p-button');
                spectator.click(closeButton);

                expect(spectator.component.$isFullscreen()).toBe(false);
            });
        });
    });

    describe('Message Handling', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [formControlName]="field.variable"
                        [inode]="contentlet?.inode"
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
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: { type: 'toggleFullscreen' },
                    origin: window.location.origin
                })
            );
            expect(spectator.component.$isFullscreen()).toBe(true);
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
    });

    describe('Form Bridge Integration', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-custom-field
                        [field]="field"
                        [formControlName]="field.variable"
                        [inode]="contentlet?.inode"
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

        it('should send form loaded message on iframe load', () => {
            spectator.fixture.whenStable().then(() => {
                const iframe = spectator.query(
                    byTestId('custom-field-iframe')
                ) as HTMLIFrameElement;
                const postMessageSpy = jest.spyOn(iframe.contentWindow, 'postMessage');
                spectator.component.onIframeLoad();

                expect(postMessageSpy).toHaveBeenCalledWith(
                    { type: 'dotcms:form:loaded' },
                    window.location.origin
                );
            });
        });
    });
});
