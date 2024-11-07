import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { ElementRef } from '@angular/core';

import { INLINE_CONTENT_STYLES, InlineEditService } from './inline-edit.service';

import { InlineEditingContentletDataset } from '../../edit-ema-editor/components/ema-page-dropzone/types';

describe('InlineEditService', () => {
    let spectator: SpectatorService<InlineEditService>;
    const createService = createServiceFactory(InlineEditService);

    beforeEach(() => (spectator = createService()));

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
});
