import { jest } from '@jest/globals';
import { createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator/jest';
import { BehaviorSubject, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeWYSIWYGField } from '@dotcms/utils-testing';

import { DotWysiwygTinymceComponent } from './dot-wysiwyg-tinymce.component';
import { DotWysiwygTinymceService } from './service/dot-wysiwyg-tinymce.service';

import { DEFAULT_TINYMCE_CONFIG } from '../../dot-edit-content-wysiwyg-field.constant';
import { DotWysiwygPluginService } from '../../dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

const mockSystemWideConfig = { systemWideOption: 'value' };

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    hasError = false;
}

const WYSIWYG_MOCK = createFakeWYSIWYGField({
    values: '<p>HELLO</p>',
    variable: 'variable'
});

describe('DotWysiwygTinymceComponent', () => {
    let spectator: SpectatorHost<DotWysiwygTinymceComponent, MockFormComponent>;
    let dotWysiwygPluginService: DotWysiwygPluginService;

    const createHost = createHostFactory({
        component: DotWysiwygTinymceComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        providers: [
            mockProvider(DotUploadFileService),
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        componentProviders: [
            mockProvider(DotWysiwygPluginService),
            mockProvider(DotWysiwygTinymceService, {
                getProps: jest.fn().mockReturnValue(of(mockSystemWideConfig))
            })
        ]
    });

    it('should initialize editor with correct configuration', () => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-wysiwyg-tinymce [field]="field" [hasError]="hasError" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [WYSIWYG_MOCK.variable]: new FormControl()
                    }),
                    field: WYSIWYG_MOCK,
                    hasError: false
                }
            }
        );

        dotWysiwygPluginService = spectator.inject(DotWysiwygPluginService, true);

        expect(spectator.component.$wideConfig()).toEqual(mockSystemWideConfig);

        const expectedConfiguration = {
            ...DEFAULT_TINYMCE_CONFIG,
            ...mockSystemWideConfig,
            setup: (editor) => dotWysiwygPluginService.initializePlugins(editor)
        };

        spectator.detectChanges();

        expect(JSON.stringify(spectator.component.$editorConfig())).toEqual(
            JSON.stringify(expectedConfiguration)
        );
    });

    it('should set the system wide props', fakeAsync(() => {
        const newSystemWideConfig = { systemWideOption: 'new_value' };

        const propsSubject = new BehaviorSubject(mockSystemWideConfig);

        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-wysiwyg-tinymce [field]="field" [hasError]="hasError" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [WYSIWYG_MOCK.variable]: new FormControl()
                    }),
                    field: WYSIWYG_MOCK,
                    hasError: false
                },
                providers: [
                    mockProvider(DotWysiwygTinymceService, {
                        getProps: jest.fn().mockReturnValue(propsSubject)
                    })
                ]
            }
        );

        spectator.detectChanges();

        tick(100);

        expect(JSON.stringify(spectator.component.$editorConfig())).toEqual(
            JSON.stringify({
                ...DEFAULT_TINYMCE_CONFIG,
                ...mockSystemWideConfig,
                setup: (editor) => dotWysiwygPluginService.initializePlugins(editor)
            })
        );

        propsSubject.next(newSystemWideConfig);
        tick(500);
        spectator.detectChanges();

        expect(JSON.stringify(spectator.component.$editorConfig())).toEqual(
            JSON.stringify({
                ...DEFAULT_TINYMCE_CONFIG,
                ...newSystemWideConfig,
                setup: (editor) => dotWysiwygPluginService.initializePlugins(editor)
            })
        );
    }));

    it('should parse custom props from field variables', () => {
        const fieldVariables = [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                fieldId: '1',
                id: '1',
                key: 'tinymceprops',
                value: '{ "toolbar1": "undo redo"}'
            }
        ];

        const fieldWithVariables = {
            ...WYSIWYG_MOCK,
            fieldVariables
        };

        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-wysiwyg-tinymce [field]="field" [hasError]="hasError" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [fieldWithVariables.variable]: new FormControl()
                    }),
                    field: fieldWithVariables,
                    hasError: false
                }
            }
        );

        spectator.detectChanges();

        expect(JSON.stringify(spectator.component.$editorConfig())).toEqual(
            JSON.stringify({
                ...DEFAULT_TINYMCE_CONFIG,
                ...mockSystemWideConfig,
                ...{ toolbar1: 'undo redo' },
                setup: (editor) => dotWysiwygPluginService.initializePlugins(editor)
            })
        );
    });
});
