import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { MockComponent } from 'ng-mocks';
import { Editor } from 'tinymce';

import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { CUSTOMER_ACTIONS, postMessageToEditor } from '@dotcms/client';

import { DotEditableTextComponent } from './dot-editable-text.component';

import { DOTCMS_CLIENT_TOKEN } from '../../tokens/client';
import { dotcmsContentletMock } from '../../utils/testing.utils';

// Mock @dotcms/client module
jest.mock('@dotcms/client', () => ({
    ...jest.requireActual('@dotcms/client'), // This line ensures other imports from @dotcms/client work as expected
    isInsideEditor: jest.fn().mockReturnValue(true), // Mock isInsideEditor to always return true
    postMessageToEditor: jest.fn()
}));

const TINYMCE_EDITOR_MOCK: unknown = {
    focus: jest.fn(),
    isDirty: () => false,
    hasFocus: () => false
};

const TINYMCE_EDITOR_PROPERTY_MOCK = {
    get: jest.fn(() => TINYMCE_EDITOR_MOCK as Editor)
};

describe('DotEditableTextComponent', () => {
    let spectator: Spectator<DotEditableTextComponent>;

    const createComponent = createComponentFactory({
        component: DotEditableTextComponent,
        declarations: [MockComponent(EditorComponent)],
        providers: [
            {
                provide: TINYMCE_SCRIPT_SRC,
                useValue: 'tinymce/tinymce.min.js'
            },
            {
                provide: DOTCMS_CLIENT_TOKEN,
                useValue: {
                    dotcmsUrl: 'http://localhost:8080'
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: dotcmsContentletMock,
                fieldName: 'title'
            },
            detectChanges: false
        });

        spectator.detectChanges();
        // Mock the editor property of the EditorComponent
        // We need to test that the methods of the editor are called
        // We do not care about how the editor handles the calls under the hood
        Object.defineProperty(
            spectator.component.editorComponent,
            'editor',
            TINYMCE_EDITOR_PROPERTY_MOCK
        );
    });

    describe('events', () => {
        describe('Window Message', () => {
            let focusSpy: jest.SpyInstance;
            beforeEach(() => {
                focusSpy = jest.spyOn(spectator.component.editorComponent.editor, 'focus');
            });

            it("should focus on the editor when the message is 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS'", () => {
                window.dispatchEvent(
                    new MessageEvent('message', {
                        data: { name: 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS' }
                    })
                );
                spectator.detectChanges();
                expect(focusSpy).toHaveBeenCalled();
            });

            it("should not focus on the editor when the message is not 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS'", () => {
                window.dispatchEvent(
                    new MessageEvent('message', {
                        data: { name: 'ANOTHER_EVENT' }
                    })
                );
                spectator.detectChanges();
                expect(focusSpy).not.toHaveBeenCalled();
            });
        });

        describe('mousedown', () => {
            let event: MouseEvent;
            let editorDebugElement: DebugElement;

            beforeEach(() => {
                spectator.setInput('contentlet', {
                    ...dotcmsContentletMock,
                    onNumberOfPages: 2 // This will need to be overridden in tests where a different value is required
                });
                spectator.detectChanges();

                event = new MouseEvent('mousedown');
                editorDebugElement = spectator.debugElement.query(By.directive(EditorComponent));

                jest.spyOn(event, 'stopPropagation');
                jest.spyOn(event, 'preventDefault');
            });

            it('should postMessage the UVE if the content is in multiple pages', () => {
                const customEvent = {
                    event,
                    editor: TINYMCE_EDITOR_MOCK
                };

                spectator.triggerEventHandler(editorDebugElement, 'onMouseDown', customEvent);

                const payload = {
                    dataset: {
                        fieldName: 'title',
                        inode: dotcmsContentletMock.inode,
                        language: dotcmsContentletMock.languageId
                    }
                };

                expect(postMessageToEditor).toHaveBeenCalledWith({
                    action: CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING,
                    payload
                });
                expect(event.stopPropagation).toHaveBeenCalled();
                expect(event.preventDefault).toHaveBeenCalled();
            });

            it('should not postMessage the UVE if the content is in a single page', () => {
                spectator.setInput('contentlet', {
                    ...dotcmsContentletMock,
                    onNumberOfPages: 1
                });
                spectator.detectChanges();

                const customEvent = {
                    event,
                    editor: TINYMCE_EDITOR_MOCK
                };
                spectator.triggerEventHandler(editorDebugElement, 'onMouseDown', customEvent);
                expect(postMessageToEditor).not.toHaveBeenCalled();
                expect(event.stopPropagation).not.toHaveBeenCalled();
                expect(event.preventDefault).not.toHaveBeenCalled();
            });

            it('should not postMessage the UVE if the editor is already focus', () => {
                const hasFocusSpy = jest
                    .spyOn(spectator.component.editorComponent.editor, 'hasFocus')
                    .mockReturnValue(true);

                spectator.detectChanges();

                const customEvent = {
                    event,
                    editor: TINYMCE_EDITOR_MOCK
                };
                spectator.triggerEventHandler(editorDebugElement, 'onMouseDown', customEvent);
                expect(postMessageToEditor).not.toHaveBeenCalled();
                expect(event.stopPropagation).not.toHaveBeenCalled();
                expect(event.preventDefault).not.toHaveBeenCalled();
                expect(hasFocusSpy).toHaveBeenCalled();
            });
        });
    });

    afterEach(() => {
        jest.clearAllMocks(); // Clear all mocks to avoid side effects from other tests
    });
});
