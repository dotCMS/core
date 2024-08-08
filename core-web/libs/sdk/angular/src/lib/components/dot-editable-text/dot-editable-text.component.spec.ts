import { SpyObject } from '@ngneat/spectator';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { MockComponent } from 'ng-mocks';
import { Editor } from 'tinymce';

import { DebugElement, ElementRef, Renderer2, SecurityContext } from '@angular/core';
import { By, DomSanitizer } from '@angular/platform-browser';

import * as dotcmsClient from '@dotcms/client';

import { DotEditableTextComponent } from './dot-editable-text.component';
import { TINYMCE_CONFIG } from './utils';

import { dotcmsContentletMock } from '../../utils/testing.utils';

const { CUSTOMER_ACTIONS, postMessageToEditor } = dotcmsClient;

// Mock @dotcms/client module
jest.mock('@dotcms/client', () => ({
    ...jest.requireActual('@dotcms/client'),
    isInsideEditor: jest.fn().mockImplementation(() => true),
    postMessageToEditor: jest.fn(),
    DotCmsClient: {
        dotcmsUrl: 'http://localhost:8080'
    }
}));

const mockedDotcmsClient = dotcmsClient as jest.Mocked<typeof dotcmsClient>;

const TINYMCE_EDITOR_MOCK: unknown = {
    focus: jest.fn(),
    getContent: (_data: unknown) => '',
    isDirty: () => false,
    hasFocus: () => false,
    setContent: jest.fn()
};

const TINYMCE_EDITOR_PROPERTY_MOCK = {
    get: jest.fn(() => TINYMCE_EDITOR_MOCK as Editor)
};

const mockEditorFn = (spectator: Spectator<DotEditableTextComponent>) => {
    // Mock the editor property of the EditorComponent
    // We need to test that the methods of the editor are called
    // We do not care about how the editor handles the calls under the hood
    Object.defineProperty(
        spectator.component.editorComponent,
        'editor',
        TINYMCE_EDITOR_PROPERTY_MOCK
    );
};

describe('DotEditableTextComponent', () => {
    let spectator: Spectator<DotEditableTextComponent>;

    const createComponent = createComponentFactory({
        component: DotEditableTextComponent,
        declarations: [MockComponent(EditorComponent)],
        providers: [
            Renderer2,
            {
                provide: DomSanitizer,
                useValue: {
                    bypassSecurityTrustHtml: () => '',
                    sanitize: () => ''
                }
            },
            {
                provide: ElementRef,
                useValue: {
                    nativeElement: document.createElement('div')
                }
            },
            {
                provide: TINYMCE_SCRIPT_SRC,
                useValue: 'tinymce/tinymce.min.js'
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
    });

    describe('Outside Editor', () => {
        let renderer2: SpyObject<Renderer2>;
        let elementRef: SpyObject<ElementRef>;
        let sanitizer: SpyObject<DomSanitizer>;

        beforeEach(() => {
            jest.spyOn(mockedDotcmsClient, 'isInsideEditor').mockReturnValue(false);
            renderer2 = spectator.inject(Renderer2, true);
            elementRef = spectator.inject(ElementRef, true);
            sanitizer = spectator.inject(DomSanitizer, true);
        });

        describe('Template', () => {
            it('Should insert safe HTML content using innerHTML', () => {
                const safeHtml = dotcmsContentletMock.title + 'Safe';
                const spyRender2 = jest.spyOn(renderer2, 'setProperty');
                const spybypassSecurityTrustHtml = jest
                    .spyOn(sanitizer, 'bypassSecurityTrustHtml')
                    .mockReturnValue(safeHtml);
                const spySanitze = jest.spyOn(sanitizer, 'sanitize').mockReturnValue(safeHtml);

                spectator.detectChanges();

                const nativeElement = elementRef.nativeElement;
                const componentHTML = spectator.debugElement.nativeElement.innerHTML;

                expect(componentHTML).toBe(safeHtml);
                expect(spybypassSecurityTrustHtml).toHaveBeenCalledWith(dotcmsContentletMock.title);
                expect(spySanitze).toHaveBeenCalledWith(SecurityContext.HTML, safeHtml);
                expect(spyRender2).toHaveBeenCalledWith(nativeElement, 'innerHTML', safeHtml);
            });

            it('Should not initialize the editor', () => {
                spectator.detectChanges();

                const editorComponent = spectator.query(EditorComponent);
                expect(editorComponent).toBeNull();
            });
        });
    });

    describe('Inside Editor', () => {
        beforeEach(() => {
            jest.spyOn(mockedDotcmsClient, 'isInsideEditor').mockReturnValue(true);
        });

        it('should set content with the right format when the contentlet changes', () => {
            spectator.detectChanges();
            mockEditorFn(spectator);

            const editorComponent = spectator.query(EditorComponent) as EditorComponent;
            const spySetContent = jest.spyOn(editorComponent.editor, 'setContent');

            spectator.setInput('contentlet', {
                ...dotcmsContentletMock,
                title: 'New title'
            });
            spectator.detectChanges();
            expect(spySetContent).toHaveBeenCalledWith('New title', { format: 'text' });
        });

        describe('Configuration', () => {
            describe('Editor Configuration', () => {
                it('should set a plain mode by default', () => {
                    spectator.setInput('mode', 'plain');
                    spectator.detectChanges();
                    const editorComponent = spectator.query(EditorComponent);

                    spectator.detectChanges();
                    expect(spectator.component.mode).toBe('plain');
                    expect(editorComponent?.init).toEqual({
                        ...TINYMCE_CONFIG['plain'],
                        base_url: 'http://localhost:8080/ext/tinymcev7'
                    });
                });

                it('should set a minimal mode when the mode is set to minimal', () => {
                    spectator.setInput('mode', 'minimal');
                    spectator.detectChanges();

                    const editorComponent = spectator.query(EditorComponent);

                    expect(spectator.component.mode).toBe('minimal');
                    expect(editorComponent?.init).toEqual({
                        ...TINYMCE_CONFIG['minimal'],
                        base_url: 'http://localhost:8080/ext/tinymcev7'
                    });
                });

                it('should set a full mode when the mode is set to full', () => {
                    spectator.setInput('mode', 'full');
                    spectator.detectChanges();

                    const editorComponent = spectator.query(EditorComponent);

                    expect(spectator.component.mode).toBe('full');
                    expect(editorComponent?.init).toEqual({
                        ...TINYMCE_CONFIG['full'],
                        base_url: 'http://localhost:8080/ext/tinymcev7'
                    });
                });
            });

            describe('format', () => {
                let getContentSpy: jest.SpyInstance;
                let editorDebugElement: DebugElement;
                let customEvent: {
                    event: FocusEvent;
                    editor: unknown;
                };

                beforeEach(() => {
                    spectator.detectChanges();
                    mockEditorFn(spectator);

                    const editor = spectator.component.editorComponent.editor;
                    getContentSpy = jest.spyOn(editor, 'getContent');
                    customEvent = {
                        event: new FocusEvent('focusout'),
                        editor: TINYMCE_EDITOR_MOCK
                    };
                    editorDebugElement = spectator.debugElement.query(
                        By.directive(EditorComponent)
                    );
                });

                it('should get the content as text by default', () => {
                    spectator.triggerEventHandler(editorDebugElement, 'onFocusOut', customEvent);
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                });

                it('should get the content as text when format is set to text', () => {
                    spectator.setInput('format', 'text');
                    spectator.detectChanges();
                    spectator.triggerEventHandler(editorDebugElement, 'onFocusOut', customEvent);
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                });

                it('should get the content as html when format is set to html', () => {
                    spectator.setInput('format', 'html');
                    spectator.detectChanges();
                    spectator.triggerEventHandler(editorDebugElement, 'onFocusOut', customEvent);
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'html' });
                });
            });
        });

        describe('events', () => {
            beforeEach(() => {
                spectator.detectChanges();
                mockEditorFn(spectator);
            });

            describe('Window Message', () => {
                let focusSpy: jest.SpyInstance;
                beforeEach(() => {
                    focusSpy = jest.spyOn(spectator.component.editorComponent.editor, 'focus');
                });

                it("should focus on the editor when the message is 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS'", () => {
                    window.dispatchEvent(
                        new MessageEvent('message', {
                            data: {
                                name: 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS',
                                payload: {
                                    oldInode: dotcmsContentletMock.inode,
                                    inode: dotcmsContentletMock.inode
                                }
                            }
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
                let customEvent: {
                    event: MouseEvent;
                    editor: unknown;
                };

                beforeEach(() => {
                    spectator.setInput('contentlet', {
                        ...dotcmsContentletMock,
                        onNumberOfPages: 2 // This will need to be overridden in tests where a different value is required
                    });
                    spectator.detectChanges();

                    event = new MouseEvent('mousedown');
                    customEvent = { event, editor: TINYMCE_EDITOR_MOCK };
                    editorDebugElement = spectator.debugElement.query(
                        By.directive(EditorComponent)
                    );

                    jest.spyOn(event, 'stopPropagation');
                    jest.spyOn(event, 'preventDefault');
                });

                it('should postMessage the UVE if the content is in multiple pages', () => {
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
                    spectator.triggerEventHandler(editorDebugElement, 'onMouseDown', customEvent);
                    expect(postMessageToEditor).not.toHaveBeenCalled();
                    expect(event.stopPropagation).not.toHaveBeenCalled();
                    expect(event.preventDefault).not.toHaveBeenCalled();
                    expect(hasFocusSpy).toHaveBeenCalled();
                });
            });

            describe('focusout', () => {
                let isDirtySpy: jest.SpyInstance;
                let getContentSpy: jest.SpyInstance;
                let event: FocusEvent;
                let editorDebugElement: DebugElement;
                let customEvent: {
                    event: FocusEvent;
                    editor: unknown;
                };

                beforeEach(() => {
                    const editor = spectator.component.editorComponent.editor;
                    isDirtySpy = jest.spyOn(editor, 'isDirty');
                    getContentSpy = jest.spyOn(editor, 'getContent');

                    event = new FocusEvent('focusout');
                    customEvent = { event, editor: TINYMCE_EDITOR_MOCK };

                    editorDebugElement = spectator.debugElement.query(
                        By.directive(EditorComponent)
                    );
                });

                it('should not postMessage the UVE if the editor is not dirty', () => {
                    isDirtySpy.mockReturnValue(false);
                    getContentSpy.mockReturnValue("I'm not dirty");

                    spectator.detectChanges();

                    spectator.triggerEventHandler(editorDebugElement, 'onFocusOut', customEvent);

                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(postMessageToEditor).not.toHaveBeenCalled();
                });

                it('should not postMessage the UVE if the content did not change', () => {
                    isDirtySpy.mockReturnValue(true);
                    getContentSpy.mockReturnValue(dotcmsContentletMock.title);

                    spectator.detectChanges();
                    spectator.triggerEventHandler(editorDebugElement, 'onFocusOut', customEvent);

                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(postMessageToEditor).not.toHaveBeenCalled();
                });

                it('should postMessage the UVE if the content changed', () => {
                    isDirtySpy.mockReturnValue(true);
                    getContentSpy.mockReturnValue('New content');

                    spectator.detectChanges();
                    spectator.triggerEventHandler(editorDebugElement, 'onFocusOut', customEvent);

                    const postMessageData = {
                        action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
                        payload: {
                            content: 'New content',
                            dataset: {
                                inode: dotcmsContentletMock.inode,
                                langId: dotcmsContentletMock.languageId,
                                fieldName: 'title'
                            }
                        }
                    };

                    expect(isDirtySpy).toHaveBeenCalled();
                    expect(getContentSpy).toHaveBeenCalledWith({ format: 'text' });
                    expect(postMessageToEditor).toHaveBeenCalledWith(postMessageData);
                });
            });
        });
    });

    afterEach(() => jest.clearAllMocks()); // Clear all mocks to avoid side effects from other tests
});
