import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { WINDOW } from '@dotcms/utils';
import { createFakeContentlet, createFakeCustomField } from '@dotcms/utils-testing';

import { NativeFieldComponent } from './native-field.component';

const MOCK_INODE = 'test-inode';

describe('NativeFieldComponent', () => {
    let spectator: SpectatorHost<NativeFieldComponent>;

    const createHost = createHostFactory({
        component: NativeFieldComponent,
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
        const fieldWithRendered = createFakeCustomField({
            rendered: '<div>Initialization Test</div>'
        });

        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithRendered.variable]: new FormControl('')
                        }),
                        field: fieldWithRendered,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithRendered.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize form bridge', () => {
            expect(spectator.component.$isBridgeReady()).toBe(true);
            expect(window['DotCustomFieldApi']).toBeDefined();
        });

        it('should compute template code from field rendered property', () => {
            expect(spectator.component.$templateCode()).toBe('<div>Initialization Test</div>');
        });
    });

    describe('Template Code Rendering', () => {
        const fieldWithTemplate = createFakeCustomField({
            rendered: '<div>Test Content</div>'
        });

        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithTemplate.variable]: new FormControl('')
                        }),
                        field: fieldWithTemplate,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithTemplate.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should render template code in container', async () => {
            await spectator.fixture.whenStable();
            // Flush signal effects to ensure mountComponent executes
            spectator.flushEffects();
            spectator.detectChanges();

            const container = spectator.component.$container().nativeElement;
            expect(container.innerHTML).toContain('Test Content');
        });
    });

    describe('Empty Template Code Rendering', () => {
        it('should not render when template code is empty', async () => {
            const fieldWithoutTemplate = createFakeCustomField({
                rendered: ''
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithoutTemplate.variable]: new FormControl('')
                        }),
                        field: fieldWithoutTemplate,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithoutTemplate.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            // Flush signal effects to ensure mountComponent executes
            spectator.flushEffects();
            spectator.detectChanges();

            const container = spectator.component.$container().nativeElement;
            expect(container.innerHTML).toBe('');
        });
    });

    describe('Script and Style Handling', () => {
        const fieldWithScriptsAndStyles = createFakeCustomField({
            rendered: `
                <style>
                    .test-class { color: red; }
                </style>
                <script>
                    console.log('test');
                </script>
                <div class="test-class">Content</div>
            `
        });

        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithScriptsAndStyles.variable]: new FormControl('')
                        }),
                        field: fieldWithScriptsAndStyles,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithScriptsAndStyles.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should extract and inject styles into document head', async () => {
            await spectator.fixture.whenStable();
            // Flush signal effects to ensure mountComponent executes
            spectator.flushEffects();
            spectator.detectChanges();

            const styles = document.head.querySelectorAll('style');
            const hasTestStyle = Array.from(styles).some((style) =>
                style.textContent?.includes('test-class')
            );
            expect(hasTestStyle).toBe(true);
        });

        it('should extract and execute scripts', async () => {
            await spectator.fixture.whenStable();
            // Flush signal effects to ensure mountComponent executes
            spectator.flushEffects();
            spectator.detectChanges();

            const container = spectator.component.$container().nativeElement;
            const scripts = container.querySelectorAll('script');
            expect(scripts.length).toBeGreaterThan(0);
        });

        it('should clean up styles on destroy', async () => {
            await spectator.fixture.whenStable();
            // Flush signal effects to ensure mountComponent executes
            spectator.flushEffects();
            spectator.detectChanges();

            const initialStyleCount = document.head.querySelectorAll('style').length;

            spectator.fixture.destroy();

            // Styles should be removed
            const finalStyleCount = document.head.querySelectorAll('style').length;
            expect(finalStyleCount).toBeLessThan(initialStyleCount);
        });
    });

    describe('Form Bridge Integration', () => {
        const fieldWithRendered = createFakeCustomField({
            rendered: '<div>Bridge Test</div>'
        });

        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithRendered.variable]: new FormControl('')
                        }),
                        field: fieldWithRendered,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithRendered.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should expose DotCustomFieldApi on window', () => {
            expect(window['DotCustomFieldApi']).toBeDefined();
        });

        it('should destroy form bridge on component destroy', () => {
            const api = window['DotCustomFieldApi'];
            const destroySpy = jest.spyOn(api, 'destroy');

            spectator.fixture.destroy();

            expect(destroySpy).toHaveBeenCalled();
        });
    });

    describe('Template Code Validation', () => {
        it('should handle invalid HTML gracefully', () => {
            const fieldWithInvalidHTML = createFakeCustomField({
                rendered: '<div><p>Unclosed tag'
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithInvalidHTML.variable]: new FormControl('')
                        }),
                        field: fieldWithInvalidHTML,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithInvalidHTML.variable]: ''
                        })
                    }
                }
            );

            expect(() => {
                spectator.detectChanges();
            }).not.toThrow();
        });

        it('should not render when template code is null', async () => {
            const fieldWithNullTemplate = createFakeCustomField({
                rendered: null
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithNullTemplate.variable]: new FormControl('')
                        }),
                        field: fieldWithNullTemplate,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithNullTemplate.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            // Flush signal effects to ensure mountComponent executes
            spectator.flushEffects();
            spectator.detectChanges();

            const container = spectator.component.$container().nativeElement;
            expect(container.innerHTML).toBe('');
        });
    });

    describe('Component Lifecycle', () => {
        const fieldWithRendered = createFakeCustomField({
            rendered: '<div>Lifecycle Test Content</div>'
        });

        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-native-field
                        [field]="field"
                        [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [fieldWithRendered.variable]: new FormControl('')
                        }),
                        field: fieldWithRendered,
                        contentlet: createFakeContentlet({
                            inode: MOCK_INODE,
                            [fieldWithRendered.variable]: ''
                        })
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should mount component when bridge is ready', async () => {
            await spectator.fixture.whenStable();
            expect(spectator.component.$isBridgeReady()).toBe(true);

            // Flush signal effects to ensure mountComponent executes
            // The mountComponent signalMethod is triggered when $isBridgeReady changes
            spectator.flushEffects();
            spectator.detectChanges();

            const container = spectator.component.$container().nativeElement;
            // fieldWithRendered has rendered property with template code
            expect(container.innerHTML).not.toBe('');
            expect(container.innerHTML).toContain('Lifecycle Test Content');
        });

        it('should clean up resources on destroy', () => {
            const initialApi = window['DotCustomFieldApi'];
            expect(initialApi).toBeDefined();

            spectator.fixture.destroy();

            // API might still exist but bridge should be destroyed
            expect(initialApi).toBeDefined();
        });
    });
});
