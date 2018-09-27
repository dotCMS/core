import { Injectable } from '@angular/core';
import { EDIT_PAGE_JS, EDIT_PAGE_JS_DOJO_REQUIRE } from './iframe-edit-mode.js';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';

const API_ROOT_PATH = '/html/js/dragula-3.7.2';

/**
 * Util class for init the dragula API.
 * for more information see: https://github.com/bevacqua/dragula
 */
@Injectable()
export class DotDragDropAPIHtmlService {
    constructor(private dotDOMHtmlUtilService: DotDOMHtmlUtilService) {}

    /**
     * Init the edit-content's drag and drop context, this make the follow steps:
     * - Load the css dragula file
     * - Load the js dragula file, either with required (if DOJO is present) or directly.
     * - Inject dragula init code from iframe-edit-mode.js
     */
    public initDragAndDropContext(iframe: any): void {
        const doc = iframe.contentDocument || iframe.contentWindow.document;
        const dragulaCSSElement = this.dotDOMHtmlUtilService.createLinkElement(
            `${API_ROOT_PATH}/dragula.min.css`
        );

        doc.head.appendChild(dragulaCSSElement);
        const dragulaJSElement = this.dotDOMHtmlUtilService.creatExternalScriptElement(
            `${API_ROOT_PATH}/dragula.min.js`,
            () => this.initDragula(doc)
        );
        // If the page has DOJO, we need to inject the Dragula dependency with require.
        if (iframe.contentWindow.hasOwnProperty('dojo')) {
            doc.body.appendChild(
                this.dotDOMHtmlUtilService.createInlineScriptElement(EDIT_PAGE_JS_DOJO_REQUIRE)
            );
        } else {
            doc.body.appendChild(dragulaJSElement);
        }
    }

    private initDragula(doc: any): any {
        const dragAndDropScript = this.dotDOMHtmlUtilService.createInlineScriptElement(
            EDIT_PAGE_JS
        );
        doc.body.appendChild(dragAndDropScript);
    }
}
