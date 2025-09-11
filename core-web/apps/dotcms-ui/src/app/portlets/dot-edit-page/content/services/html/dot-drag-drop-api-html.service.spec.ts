/* eslint-disable @typescript-eslint/no-explicit-any */

import { TestBed, waitForAsync } from '@angular/core/testing';

import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';
import { DotDragDropAPIHtmlService } from './dot-drag-drop-api-html.service';
import EDIT_MODE_DRAG_DROP, { EDIT_PAGE_JS_DOJO_REQUIRE } from './libraries';

// Mock DRAGULA_CSS to avoid JSDOM issues
const DRAGULA_CSS = 'mock-css-content';

describe('DotDragDropAPIHtmlService', () => {
    let service: DotDragDropAPIHtmlService;

    const iframe = document.createElement('iframe');
    document.body.appendChild(iframe);
    const doc = iframe.contentWindow.document;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [DotDragDropAPIHtmlService, DotDOMHtmlUtilService],
            imports: []
        });

        service = TestBed.inject(DotDragDropAPIHtmlService);

        // Mock the getDragulaCSS method to return our mock CSS
        const mockStyleElement = document.createElement('style');
        mockStyleElement.innerHTML = DRAGULA_CSS;
        jest.spyOn(service as any, 'getDragulaCSS').mockReturnValue(mockStyleElement);

        jest.spyOn(doc.head, 'appendChild');
        jest.spyOn(doc.body, 'appendChild');
    }));

    it('should include drag and drop css and js', () => {
        service.initDragAndDropContext(iframe);

        expect(doc.head.appendChild).toHaveBeenCalledWith(
            expect.objectContaining({
                tagName: 'STYLE',
                innerHTML: DRAGULA_CSS
            })
        );

        expect(doc.body.appendChild).toHaveBeenCalledWith(
            expect.objectContaining({
                tagName: 'SCRIPT',
                innerHTML: EDIT_MODE_DRAG_DROP
            })
        );
    });

    it('should include drag and drop css and js for DOJO', () => {
        jest.spyOn<any>(iframe.contentWindow, 'hasOwnProperty').mockReturnValue(true);
        service.initDragAndDropContext(iframe);

        expect(doc.body.appendChild).toHaveBeenCalledWith(
            expect.objectContaining({
                tagName: 'SCRIPT',
                innerHTML: EDIT_PAGE_JS_DOJO_REQUIRE
            })
        );
    });
});
