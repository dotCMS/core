import { Injectable } from '@angular/core';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';

import DRAGULA_CSS from './libraries/dragula.css.js';
import EDIT_PAGE_DRAG_DROP, { EDIT_PAGE_JS_DOJO_REQUIRE } from './libraries/index.js';

/**
 * Util class for init the dragula API.
 * for more information see: https://github.com/bevacqua/dragula
 */
@Injectable()
export class DotDragDropAPIHtmlService {
    constructor(private dotDOMHtmlUtilService: DotDOMHtmlUtilService) {}

    /**
     * Inject all the drag and drop code
     * 1. Dragula library
     * 2. Autoscroll library
     * 3. Custom DotCMS setup code
     *
     * @param {HTMLIFrameElement} iframe
     * @memberof DotDragDropAPIHtmlService
     */
    initDragAndDropContext(iframe: HTMLIFrameElement): void {
        const doc = iframe.contentDocument || iframe.contentWindow.document;

        const dragulaCSSElement = this.getDragulaCSS();
        doc.head.appendChild(dragulaCSSElement);

        // If the page has DOJO, we need to inject the Dragula dependency with require.
        const script = iframe.contentWindow.hasOwnProperty('dojo')
            ? this.getDojoDragAndDropScript()
            : this.getDragAndDropScript();

        doc.body.appendChild(script);
    }

    private getDragAndDropScript(): HTMLScriptElement {
        const dragAndDropScript = this.dotDOMHtmlUtilService.createInlineScriptElement(
            EDIT_PAGE_DRAG_DROP
        );

        return dragAndDropScript;
    }
    private getDojoDragAndDropScript(): HTMLScriptElement {
        const dragAndDropScript = this.dotDOMHtmlUtilService.createInlineScriptElement(
            EDIT_PAGE_JS_DOJO_REQUIRE
        );

        return dragAndDropScript;
    }

    private getDragulaCSS(): HTMLStyleElement {
        const style = document.createElement('style');
        style.innerHTML = DRAGULA_CSS;

        return style;
    }
}
