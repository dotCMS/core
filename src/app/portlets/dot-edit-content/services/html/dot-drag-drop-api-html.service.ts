import { Injectable, ElementRef } from '@angular/core';
import { EDIT_PAGE_JS } from './iframe-edit-mode.js';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';

const API_ROOT_PATH = '/html/js/dragula-3.7.2';

/**
 * Util class for init the dragula API.
 * for more information see: https://github.com/bevacqua/dragula
 */
@Injectable()
export class DotDragDropAPIHtmlService {

    constructor(private dotDOMHtmlUtilService: DotDOMHtmlUtilService) {

    }

    /**
     * Init the edit-content's drag and drop context, this make the follow steps:
     * - Load the css dragula file
     * - Load the js dragula file
     * - Inject dragula init code from iframe-edit-mode.js
     */
    public initDragAndDropContext(doc: any): void {
        const dragulaCSSElement = this.dotDOMHtmlUtilService.createLinkElement(`${API_ROOT_PATH}/dragula.min.css`);
        doc.head.appendChild(dragulaCSSElement);

        const dragulsJSElement = this.dotDOMHtmlUtilService.creatExternalScriptElement(
            `${API_ROOT_PATH}/dragula.min.js`,
            () => this.initDragula(doc)
        );

        doc.body.appendChild(dragulsJSElement);
    }

    private initDragula(doc: any): any {
        const dragAndDropScript = this.dotDOMHtmlUtilService.createInlineScriptElement(EDIT_PAGE_JS);
        doc.body.appendChild(dragAndDropScript);
    }
}
