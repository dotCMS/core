import { DotDragDropAPIHtmlService } from './dot-drag-drop-api-html.service';
import { waitForAsync } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';
import { EDIT_PAGE_JS, EDIT_PAGE_JS_DOJO_REQUIRE } from './iframe-edit-mode.js';

const jsDragulaInlineElement = {};
let lastAppendChildCallElementParam;

@Injectable()
export class MockDotDOMHtmlUtilService {
    createInlineScriptElementLastCallTextParam;

    createLinkElement(): any {
        return null;
    }

    creatExternalScriptElement(): any {
        return null;
    }

    createInlineScriptElement(text: string): any {
        this.createInlineScriptElementLastCallTextParam = text;
        return jsDragulaInlineElement;
    }
}

describe('DotDragDropAPIHtmlService', () => {
    let dotDragDropAPIHtmlService: DotDragDropAPIHtmlService;
    let dotDOMHtmlUtilService: MockDotDOMHtmlUtilService;

    const cssElement = {};
    const jsElement = {};
    let callbackFunc;
    const iframe: any = {
        contentWindow: {
            document: {
                head: {
                    appendChild(): any {
                        return null;
                    }
                },
                body: {
                    appendChild(element: any): any {
                        lastAppendChildCallElementParam = element;
                        return null;
                    }
                }
            }
        }
    };

    beforeEach(waitForAsync( () => {
        TestBed.configureTestingModule({
            providers: [
                DotDragDropAPIHtmlService,
                { provide: DotDOMHtmlUtilService, useClass: MockDotDOMHtmlUtilService }
            ],
            imports: []
        });

        dotDragDropAPIHtmlService = TestBed.get(DotDragDropAPIHtmlService);
        dotDOMHtmlUtilService = TestBed.get(DotDOMHtmlUtilService);

        spyOn(dotDOMHtmlUtilService, 'createLinkElement').and.returnValue(cssElement);
        spyOn(iframe.contentWindow.document.head, 'appendChild');

        spyOn<any>(dotDOMHtmlUtilService, 'creatExternalScriptElement').and.callFake(
            (_src, callback) => {
                callbackFunc = callback;
            }
        );
    }));

    it('should crate and set js and css draguls element', () => {
        dotDragDropAPIHtmlService.initDragAndDropContext(iframe);

        expect<any>(dotDOMHtmlUtilService.createLinkElement).toHaveBeenCalledWith(
            '/html/js/dragula-3.7.2/dragula.min.css'
        );
        expect(iframe.contentWindow.document.head.appendChild).toHaveBeenCalledWith(cssElement);

        expect<any>(dotDOMHtmlUtilService.creatExternalScriptElement).toHaveBeenCalledWith(
            '/html/js/dragula-3.7.2/dragula.min.js',
            jasmine.any(Function)
        );
        expect(iframe.contentWindow.document.head.appendChild).toHaveBeenCalledWith(jsElement);
    });

    it('should init dragula context', () => {
        dotDragDropAPIHtmlService.initDragAndDropContext(iframe);

        callbackFunc();

        expect(dotDOMHtmlUtilService.createInlineScriptElementLastCallTextParam).toEqual(
            EDIT_PAGE_JS
        );

        expect(lastAppendChildCallElementParam).toEqual(jsDragulaInlineElement);
    });

    it('should init dragula context with require import', () => {
        iframe.contentWindow.dojo = 'test';
        dotDragDropAPIHtmlService.initDragAndDropContext(iframe);

        expect(dotDOMHtmlUtilService.createInlineScriptElementLastCallTextParam).toEqual(
            EDIT_PAGE_JS_DOJO_REQUIRE
        );
        expect(lastAppendChildCallElementParam).toEqual(jsDragulaInlineElement);
    });
});
