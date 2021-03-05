import { waitForAsync } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';

import { DotDragDropAPIHtmlService } from './dot-drag-drop-api-html.service';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';
import DRAGULA_CSS from './libraries/dragula.css';
import EDIT_MODE_DRAG_DROP, { EDIT_PAGE_JS_DOJO_REQUIRE } from './libraries';

describe('DotDragDropAPIHtmlService', () => {
    let service: DotDragDropAPIHtmlService;

    const iframe = document.createElement('iframe');
    document.body.appendChild(iframe);
    const doc = iframe.contentWindow.document;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                providers: [DotDragDropAPIHtmlService, DotDOMHtmlUtilService],
                imports: []
            });

            service = TestBed.inject(DotDragDropAPIHtmlService);

            spyOn(doc.head, 'appendChild').and.callThrough();
            spyOn(doc.body, 'appendChild').and.callThrough();
        })
    );

    it('should include drag and drop css and js', () => {
        service.initDragAndDropContext(iframe);

        expect(doc.head.appendChild).toHaveBeenCalledWith(
            jasmine.objectContaining({
                tagName: 'STYLE',
                innerHTML: DRAGULA_CSS
            })
        );

        expect(doc.body.appendChild).toHaveBeenCalledWith(
            jasmine.objectContaining({
                tagName: 'SCRIPT',
                innerHTML: EDIT_MODE_DRAG_DROP
            })
        );
    });

    it('should include drag and drop css and js for DOJO', () => {
        spyOn<any>(iframe.contentWindow, 'hasOwnProperty').and.returnValue(true);
        service.initDragAndDropContext(iframe);

        expect(doc.body.appendChild).toHaveBeenCalledWith(
            jasmine.objectContaining({
                tagName: 'SCRIPT',
                innerHTML: EDIT_PAGE_JS_DOJO_REQUIRE
            })
        );
    });
});
