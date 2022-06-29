import { Injectable } from '@angular/core';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';

/**
 * Util class for init the Html2Canvas.
 * for more information see: https://html2canvas.hertzen.com/
 */
@Injectable()
export class DotHtml2CanvasDomService {
    constructor(private dotDOMHtmlUtilService: DotDOMHtmlUtilService) {}

    /**
     * Inject all the Html2Canvas code
     * 1. Html2Canvas library
     * 2. Custom DotCMS setup code
     *
     * @param {HTMLIFrameElement} iframe
     * @memberof DotDragDropAPIHtmlService
     */
    initHtml2CanvasContext(iframe: HTMLIFrameElement): void {
        const doc = iframe.contentDocument || iframe.contentWindow.document;
        this.getHtml2CanvasLibrary(doc);
        this.getHtml2CanvasScript(doc);
    }

    private getHtml2CanvasScript(iframeDocument: Document): void {
        const html2CanvasScript = `
        // BEGIN html2canvas

            function takeScreenshot(e) {
                html2canvas(document.body, {
                    // html2canvas(bodi, {
                            windowWidth: 861, // window size were html will be loaded
                        // removeContainer: false,
                        // useCORS: true,
                        // foreignObjectRendering: true,
                        // windowHeight: 1400,
                        height: e.data.height, // up to how many px you want to get
                        windowHeight: e.data.height, // up to how many px you want to get
                        width: e.data.width, // up to how many px you want to get
                        windowWidth: e.data.width, // up to how many px you want to get

                        // scale: 0.3
                    }).then((canvas) => {
                        // document.body.appendChild(canvas);
                        canvas.toBlob((blob) => {
                            const file = new File([blob], 'image.png');
                            console.log('***canvas file', file);
                            e.ports[0].postMessage({
                                message: 'Message back from the IFrame',
                                file: blob
                            });

                            // const fileURL = URL.createObjectURL(blob);
                        // window.open(fileURL);

                        });

                        // Use the transfered port to post a message back to the main frame
                });
            }

            window.addEventListener('message', function (e) {
                // Get the sent data
                // const data = e.data;
                console.log('===', e)

                if (e.data.message === 'html2canvas') {
                    takeScreenshot(e);
                }
            });

        // END html2canvas
        `;

        const dragAndDropScript =
            this.dotDOMHtmlUtilService.createInlineScriptElement(html2CanvasScript);

        iframeDocument.body.appendChild(dragAndDropScript);
    }

    private getHtml2CanvasLibrary(iframeDocument: Document): void {
        const scriptTag = document.createElement('script');

        scriptTag.src = '/html/js/html2canvas/html2canvas.min.js';
        iframeDocument.body.appendChild(scriptTag);
    }
}
