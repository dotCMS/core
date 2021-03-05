import { Injectable } from '@angular/core';

const JS_MIME_TYPE = 'text/javascript';
const CSS_MIME_TYPE = 'text/css';

/**
 * Util service to create html elements to inject into our edit mode
 *
 * @export
 * @class DotDOMHtmlUtilService
 */
@Injectable()
export class DotDOMHtmlUtilService {
    createLinkElement(href: string): HTMLLinkElement {
        const cssElement = document.createElement('link');
        cssElement.rel = 'stylesheet';
        cssElement.type = CSS_MIME_TYPE;
        cssElement.media = 'all';
        cssElement.href = href;

        return cssElement;
    }

    /**
     * Create a <style> element with the string received
     *
     * @param {string} css
     * @returns {HTMLStyleElement}
     * @memberof DotDOMHtmlUtilService
     */
    createStyleElement(css: string): HTMLStyleElement {
        const cssElement: HTMLStyleElement = document.createElement('style');
        cssElement.appendChild(document.createTextNode(css));
        return cssElement;
    }

    /**
     * Create a <script> with external url and load callback
     *
     * @param {string} src
     * @param {() => void} [onLoadCallback]
     * @returns {HTMLScriptElement}
     * @memberof DotDOMHtmlUtilService
     */
    creatExternalScriptElement(src: string, onLoadCallback?: () => void): HTMLScriptElement {
        const script = this.createScriptElement();
        script.src = src;
        script.onload = onLoadCallback;

        return script;
    }

    /**
     * Create a <script> element with inner text
     *
     * @param {string} text
     * @returns {HTMLScriptElement}
     * @memberof DotDOMHtmlUtilService
     */
    createInlineScriptElement(text: string): HTMLScriptElement {
        const script = this.createScriptElement();
        script.text = text;

        return script;
    }

    /**
     * Creates a button with the params and return the html string
     *
     * @param {string} label
     * @param {string} className
     * @param {{ [key: string]: string }} dataset
     * @returns {string}
     * @memberof DotDOMHtmlUtilService
     */
    getButtomHTML(label: string, className: string, dataset: { [key: string]: string }): string {
        // TODO look for a better way to do this
        let datasetString = '';

        // tslint:disable-next-line:forin
        for (const property in dataset) {
            datasetString += ` data-${property}="${dataset[property]}"`;
        }

        return `<button type="button" role="button"
                        ${datasetString}
                        class="${className}"
                        aria-label="${label}">
                    ${label}
                </button>`;
    }

    private createScriptElement(): HTMLScriptElement {
        const script: HTMLScriptElement = document.createElement('script');
        script.type = JS_MIME_TYPE;

        return script;
    }
}
