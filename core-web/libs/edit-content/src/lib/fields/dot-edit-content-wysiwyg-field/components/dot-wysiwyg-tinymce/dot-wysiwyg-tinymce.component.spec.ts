import { jest } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotWysiwygTinymceComponent } from './dot-wysiwyg-tinymce.component';
import { DotWysiwygTinymceService } from './service/dot-wysiwyg-tinymce.service';

import { createFormGroupDirectiveMock, WYSIWYG_MOCK } from '../../../../utils/mocks';
import { DEFAULT_TINYMCE_CONFIG } from '../../dot-edit-content-wysiwyg-field.constant';
import { DotWysiwygPluginService } from '../../dot-wysiwyg-plugin/dot-wysiwyg-plugin.service';

const mockSystemWideConfig = { systemWideOption: 'value' };

describe('DotWysiwygTinymceComponent', () => {
    let spectator: Spectator<DotWysiwygTinymceComponent>;
    let dotWysiwygPluginService: DotWysiwygPluginService;
    let dotWysiwygTinymceService: SpyObject<DotWysiwygTinymceService>;

    const createComponent = createComponentFactory({
        component: DotWysiwygTinymceComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            },
            {
                provide: DotWysiwygPluginService,
                useValue: {
                    initializePlugins: jest.fn()
                }
            },
            mockProvider(DotWysiwygTinymceService, {
                getProps: jest.fn().mockReturnValue(of(mockSystemWideConfig))
            })
        ],
        providers: [FormGroupDirective, provideHttpClient(), provideHttpClientTesting()]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK
            } as unknown,
            detectChanges: false
        });

        dotWysiwygPluginService = spectator.inject(DotWysiwygPluginService, true);
        dotWysiwygTinymceService = spectator.inject(DotWysiwygTinymceService, true);
    });

    it('should initialize editor with correct configuration', () => {
        const expectedConfiguration = {
            ...DEFAULT_TINYMCE_CONFIG,
            ...mockSystemWideConfig,
            setup: (editor) => dotWysiwygPluginService.initializePlugins(editor)
        };

        spectator.detectChanges();
        expect(JSON.stringify(spectator.component.$editorOptions())).toEqual(
            JSON.stringify(expectedConfiguration)
        );
    });

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

        spectator = createComponent({
            props: {
                field: {
                    ...WYSIWYG_MOCK,
                    fieldVariables
                }
            } as unknown,
            detectChanges: false
        });

        spectator.detectChanges();

        expect(JSON.stringify(spectator.component.$editorOptions())).toEqual(
            JSON.stringify({
                ...DEFAULT_TINYMCE_CONFIG,
                ...mockSystemWideConfig,
                ...{ toolbar1: 'undo redo' },
                setup: (editor) => dotWysiwygPluginService.initializePlugins(editor)
            })
        );
    });

    it('should set the system wide props', () => {
        const newSystemWideConfig = { systemWideOption: 'new_value' };

        jest.spyOn(dotWysiwygTinymceService, 'getProps').mockReturnValue(of(newSystemWideConfig));

        spectator.detectChanges();

        expect(JSON.stringify(spectator.component.$editorOptions())).toEqual(
            JSON.stringify({
                ...DEFAULT_TINYMCE_CONFIG,
                ...newSystemWideConfig,
                setup: (editor) => dotWysiwygPluginService.initializePlugins(editor)
            })
        );
    });
});
