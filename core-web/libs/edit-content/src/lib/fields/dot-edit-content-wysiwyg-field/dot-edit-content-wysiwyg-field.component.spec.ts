import { expect } from '@jest/globals';
import { byTestId } from '@ngneat/spectator';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { FormsModule } from '@angular/forms';

import { DotWysiwygMonacoComponent } from './components/dot-wysiwyg-monaco/dot-wysiwyg-monaco.component';
import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import { DotEditContentWYSIWYGFieldComponent } from './dot-edit-content-wysiwyg-field.component';
import { EditorOptions } from './dot-edit-content-wysiwyg-field.constant';

import { WYSIWYG_MOCK } from '../../utils/mocks';

describe('DotEditContentWYSIWYGFieldComponent', () => {
    let spectator: Spectator<DotEditContentWYSIWYGFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentWYSIWYGFieldComponent,
        imports: [
            FormsModule,
            MockComponent(DotWysiwygTinymceComponent),
            MockComponent(DotWysiwygMonacoComponent)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: WYSIWYG_MOCK
            } as unknown,
            detectChanges: false
        });
    });

    it('should render the editor container', () => {
        expect(spectator.query('.wysiwyg__editor')).not.toBeNull();
    });

    it('should render the controls container', () => {
        expect(spectator.query('.wysiwyg__controls')).not.toBeNull();
    });

    it('should render editor selection dropdown', () => {
        const dropdown = spectator.query(byTestId('editor-selector'));
        expect(dropdown).toBeTruthy();
        expect(spectator.component.editorOptions).toEqual(EditorOptions);
    });

    // it('should initialize with the default editor', () => {
    //     expect(spectator.component.selectedEditor()).toBe(DEFAULT_EDITOR);
    //     const editorElement = spectator.query(DotWysiwygTinymceComponent);
    //     expect(editorElement).toBeTruthy();
    // });
    //
    // it('should render TinyMCE component when TinyMCE is selected', () => {
    //     expect(spectator.component.selectedEditor).toBe(DEFAULT_EDITOR);
    //
    //     spectator.detectChanges();
    //     expect(spectator.query(DotWysiwygTinymceComponent)).toBeTruthy();
    //     expect(spectator.query(DotWysiwygMonacoComponent)).toBeNull();
    // });

    // it('should render Monaco component when Monaco is selected', () => {
    //     expect(spectator.component.selectedEditor()).toBe(DEFAULT_EDITOR);
    //
    //     const dropdown = spectator.query(byTestId('editor-selector'));
    //
    //     spectator.click(dropdown);
    //     spectator.detectChanges();
    //
    //     const option = spectator.query('.p-dropdown-item:nth-child(2)');
    //     console.info(option);
    //     spectator.click(option);
    //     spectator.detectChanges();
    //
    //     expect(spectator.query(DotWysiwygMonacoComponent)).toBeTruthy();
    //     expect(spectator.query(DotWysiwygTinymceComponent)).toBeNull();
    // });

    /// OLD TESTS

    // it('should instance WYSIWYG editor and set the correct configuration', () => {
    //     spectator.detectChanges();
    //     const editor = spectator.query(EditorComponent);
    //     expect(editor.init).toEqual({
    //         ...DEFAULT_CONFIG,
    //         theme: 'silver',
    //         setup: expect.any(Function)
    //     });
    // });
    //
    // it('should initialize Plugins when the setup method is called', () => {
    //     spectator.detectChanges();
    //     const spy = jest.spyOn(dotWysiwygPluginService, 'initializePlugins');
    //     const editor = spectator.query(EditorComponent);
    //     const mockEditor = {} as Editor;
    //     editor.init.setup(mockEditor);
    //     expect(spy).toHaveBeenCalledWith(mockEditor);
    // });
    //
    // describe('variables', () => {
    //     it('should overwrite the editor configuration with the field variables', () => {
    //         const fieldVariables = [
    //             {
    //                 clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
    //                 fieldId: '1',
    //                 id: '1',
    //                 key: 'tinymceprops',
    //                 value: '{ "toolbar1": "undo redo"}'
    //             }
    //         ];
    //
    //         spectator.setInput('field', {
    //             ...WYSIWYG_MOCK,
    //             fieldVariables
    //         });
    //
    //         const editor = spectator.query(EditorComponent);
    //         expect(editor.init).toEqual({
    //             ...DEFAULT_CONFIG,
    //             theme: 'silver',
    //             toolbar1: 'undo redo',
    //             setup: expect.any(Function)
    //         });
    //     });
    //
    //     it('should not configure theme property', () => {
    //         const fieldVariables = [
    //             {
    //                 clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
    //                 fieldId: '1',
    //                 id: '1',
    //                 key: 'tinymceprops',
    //                 value: '{theme: "modern"}'
    //             }
    //         ];
    //
    //         spectator.setInput('field', {
    //             ...WYSIWYG_MOCK,
    //             fieldVariables
    //         });
    //
    //         const editor = spectator.query(EditorComponent);
    //         expect(editor.init).toEqual({
    //             ...DEFAULT_CONFIG,
    //             theme: 'silver',
    //             setup: expect.any(Function)
    //         });
    //     });
    // });
    //
    // describe('Systemwide TinyMCE prop', () => {
    //     it('should set the systemwide TinyMCE props', () => {
    //         const SYSTEM_WIDE_CONFIG = {
    //             toolbar1: 'undo redo | bold italic',
    //             theme: 'modern'
    //         };
    //
    //         jest.spyOn(httpClient, 'get').mockReturnValue(of(SYSTEM_WIDE_CONFIG));
    //
    //         spectator.detectChanges();
    //
    //         const editor = spectator.query(EditorComponent);
    //         expect(editor.init).toEqual({
    //             ...SYSTEM_WIDE_CONFIG,
    //             theme: 'silver',
    //             setup: expect.any(Function)
    //         });
    //     });
    //
    //     it('should set default values if the systemwide TinyMCE props throws an error', () => {
    //         jest.spyOn(httpClient, 'get').mockReturnValue(throwError(null));
    //
    //         spectator.detectChanges();
    //
    //         const editor = spectator.query(EditorComponent);
    //         expect(editor.init).toEqual({
    //             ...DEFAULT_CONFIG,
    //             theme: 'silver',
    //             setup: expect.any(Function)
    //         });
    //     });
    //
    //     it('should overwrite the systemwide TinyMCE props with the field variables', () => {
    //         const SYSTEM_WIDE_CONFIG = {
    //             toolbar1: 'undo redo | bold italic'
    //         };
    //
    //         const fieldVariables = [
    //             {
    //                 clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
    //                 fieldId: '1',
    //                 id: '1',
    //                 key: 'tinymceprops',
    //                 value: '{ "toolbar1" : "undo redo" }'
    //             }
    //         ];
    //
    //         jest.spyOn(httpClient, 'get').mockReturnValue(of(SYSTEM_WIDE_CONFIG));
    //
    //         spectator.setInput('field', {
    //             ...WYSIWYG_MOCK,
    //             fieldVariables
    //         });
    //
    //         spectator.detectChanges();
    //
    //         const editor = spectator.query(EditorComponent);
    //         expect(editor.init).toEqual({
    //             ...SYSTEM_WIDE_CONFIG,
    //             theme: 'silver',
    //             toolbar1: 'undo redo',
    //             setup: expect.any(Function)
    //         });
    //     });
    // });
});
