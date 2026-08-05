import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

import { ElementRef } from '@angular/core';

import { DotCMSUVEAction } from '@dotcms/types';

import { INLINE_CONTENT_STYLES, InlineEditService } from './inline-edit.service';

import { InlineEditingContentletDataset } from '../../edit-ema-editor/components/ema-page-dropzone/types';

/**
 * Runs the service TinyMCE `setup` with a stub editor and exposes the `blur` handler
 * so tests can control `getContent` / `startContent` / `isNotDirty` before invoking blur.
 */
function bindBlurHarness(setup: (editor: TinyMceEditorStub) => void): {
    editor: TinyMceEditorStub;
    invokeBlur: () => void;
} {
    const targetElm = document.createElement('span');
    targetElm.dataset.inode = '1';
    targetElm.dataset.fieldName = 'body';

    const container = document.createElement('div');
    container.setAttribute('data-dot-object', 'container');
    const bodyElement = document.createElement('div');
    bodyElement.classList.add('active');
    container.appendChild(bodyElement);

    const editor: TinyMceEditorStub = {
        on: jest.fn(),
        targetElm,
        bodyElement,
        getContent: jest.fn().mockReturnValue(''),
        startContent: '',
        isNotDirty: true,
        target: document.createElement('div'),
        destroy: jest.fn()
    };

    setup(editor);

    const blurCall = editor.on.mock.calls.find(([evt]) => evt === 'blur');
    expect(blurCall).toBeDefined();

    const blurHandler = blurCall[1] as (e: BlurEventStub) => void;

    const invokeBlur = (): void =>
        blurHandler({
            target: editor,
            type: 'blur',
            stopImmediatePropagation: jest.fn()
        });

    return { editor, invokeBlur };
}

interface BlurEventStub {
    target: TinyMceEditorStub;
    type: string;
    stopImmediatePropagation: jest.Mock;
}

interface TinyMceEditorStub {
    on: jest.Mock;
    targetElm: HTMLElement;
    bodyElement: HTMLElement;
    getContent: jest.Mock;
    startContent: string;
    isNotDirty: boolean;
    target: HTMLElement;
    destroy: jest.Mock;
}

describe('InlineEditService', () => {
    let spectator: SpectatorService<InlineEditService>;
    const createService = createServiceFactory(InlineEditService);

    beforeEach(() => (spectator = createService()));

    it.each([
        { mode: 'minimal', label: 'minimal' },
        { mode: 'full', label: 'full' },
        { mode: '', label: 'default (minimal)' }
    ])('should pass convert_urls: false to tinymce.init ($label)', ({ mode }) => {
        const initMock = jest.fn().mockResolvedValue([]);
        const iframeWindow = {
            tinymce: { init: initMock }
        } as unknown as Window;

        spectator.service.setIframeWindow(iframeWindow);
        spectator.service.setTargetInlineMCEDataset({
            inode: '1',
            fieldName: 'body',
            language: 'en',
            mode
        });

        spectator.service.initEditor();

        expect(initMock).toHaveBeenCalledTimes(1);
        expect(initMock.mock.calls[0][0].convert_urls).toBe(false);
    });

    it('should inject inline edit', () => {
        const iframe = document.createElement('iframe');
        document.body.appendChild(iframe);
        const iframeElement = new ElementRef<HTMLIFrameElement>(iframe);

        spectator.service.injectInlineEdit(iframeElement);

        const script = iframe.contentDocument.querySelector('script[data-inline="true"]');
        const style = iframe.contentDocument.querySelector('style');

        expect(script).toBeTruthy();
        expect(style).toBeTruthy();
    });

    it('should remove inline edit', () => {
        const iframe = document.createElement('iframe');
        document.body.appendChild(iframe);
        const iframeElement = new ElementRef<HTMLIFrameElement>(iframe);

        spectator.service.injectInlineEdit(iframeElement);

        const script = iframe.contentDocument.querySelector('script[data-inline="true"]');
        const style = iframe.contentDocument.querySelector('style');

        expect(script).toBeTruthy();
        expect(style).toBeTruthy();

        spectator.service.removeInlineEdit(iframeElement);

        const scriptRemoved = iframe.contentDocument.querySelector('script[data-inline="true"]');
        const styleRemoved = iframe.contentDocument.querySelector('style');

        expect(scriptRemoved).toBeFalsy();
        expect(styleRemoved).toBeFalsy();
    });

    it('should check if contentlet is in multiple pages', () => {
        const dataset: InlineEditingContentletDataset = {
            inode: '123',
            fieldName: 'content',
            language: '',
            mode: 'full'
        };

        const iframe = document.createElement('iframe');
        document.body.appendChild(iframe);

        const targetElementMock = iframe.contentDocument.createElement('div');
        targetElementMock.setAttribute('data-inode', dataset.inode);
        targetElementMock.setAttribute('data-field-name', dataset.fieldName);

        const contentletMock = iframe.contentDocument.createElement('div');
        contentletMock.setAttribute('data-dot-object', 'contentlet');
        contentletMock.setAttribute('data-dot-on-number-of-pages', '2');
        contentletMock.appendChild(targetElementMock);

        iframe.contentDocument.body.appendChild(contentletMock);

        spectator.service.setIframeWindow(iframe.contentWindow);

        const isInMultiplePages = spectator.service['isInMultiplePages'](dataset);

        expect(isInMultiplePages).toBe(true);
    });

    it('should set the right scripts and styles', () => {
        const iframe = document.createElement('iframe');
        const iframeElement = new ElementRef<HTMLIFrameElement>(iframe);
        document.body.appendChild(iframe);
        spectator.service.injectInlineEdit(iframeElement);

        const wysiwygScript = iframe.contentDocument.querySelector(
            'script[src="/html/js/tinymce/js/tinymce/tinymce.min.js"]'
        );

        const style = iframe.contentDocument.querySelector('style');

        expect(wysiwygScript).toBeTruthy();
        expect(style.innerHTML).toBe(INLINE_CONTENT_STYLES);
    });

    it('should set target inline MCE dataset', () => {
        const dataset: InlineEditingContentletDataset = {
            inode: '123',
            fieldName: 'content',
            language: '',
            mode: 'full'
        };

        spectator.service.setTargetInlineMCEDataset(dataset);

        expect(spectator.service['$inlineEditingTargetDataset']()).toEqual(dataset);
    });

    it('should set iframe window', () => {
        const iframeWindowMock = window;

        spectator.service.setIframeWindow(iframeWindowMock);

        expect(spectator.service['$iframeWindow']()).toBe(iframeWindowMock);
    });

    describe('TinyMCE blur → update-contentlet-inline-editing', () => {
        let postMessageSpy: jest.SpyInstance;

        beforeEach(() => {
            postMessageSpy = jest.spyOn(window.parent, 'postMessage').mockImplementation();
        });

        afterEach(() => {
            postMessageSpy.mockRestore();
        });

        function initAndGetSetup(): (editor: TinyMceEditorStub) => void {
            const initMock = jest.fn().mockResolvedValue([]);
            const iframeWindow = {
                tinymce: { init: initMock }
            } as unknown as Window;

            spectator.service.setIframeWindow(iframeWindow);
            spectator.service.setTargetInlineMCEDataset({
                inode: '1',
                fieldName: 'body',
                language: 'en',
                mode: 'minimal'
            });

            spectator.service.initEditor();

            expect(initMock).toHaveBeenCalledTimes(1);

            return initMock.mock.calls[0][0].setup as (editor: TinyMceEditorStub) => void;
        }

        it('should post null payload on blur when editor is clean and HTML matches startContent', () => {
            const setup = initAndGetSetup();
            const { editor, invokeBlur } = bindBlurHarness(setup);

            const html = '<p>unchanged</p>';
            editor.getContent.mockReturnValue(html);
            editor.startContent = html;
            editor.isNotDirty = true;

            invokeBlur();

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                    payload: null
                },
                '*'
            );
            expect(editor.destroy).toHaveBeenCalledWith(false);
        });

        it('should post payload on blur when TinyMCE is still "not dirty" but HTML differs from startContent', () => {
            const setup = initAndGetSetup();
            const { editor, invokeBlur } = bindBlurHarness(setup);

            editor.startContent = '<ul><li></li></ul>';
            editor.getContent.mockReturnValue('<p></p>');
            editor.isNotDirty = true;

            invokeBlur();

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                    payload: expect.objectContaining({
                        content: '<p></p>',
                        eventType: 'blur',
                        dataset: expect.objectContaining({
                            inode: '1',
                            fieldName: 'body'
                        }),
                        isNotDirty: true
                    })
                },
                '*'
            );
        });

        it('should post payload on blur when isNotDirty is false even if HTML matches startContent', () => {
            const setup = initAndGetSetup();
            const { editor, invokeBlur } = bindBlurHarness(setup);

            const html = '<p>same</p>';
            editor.getContent.mockReturnValue(html);
            editor.startContent = html;
            editor.isNotDirty = false;

            invokeBlur();

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                    payload: expect.objectContaining({
                        content: html,
                        isNotDirty: false
                    })
                },
                '*'
            );
        });
    });
});
