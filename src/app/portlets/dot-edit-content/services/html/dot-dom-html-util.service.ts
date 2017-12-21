import { Injectable } from '@angular/core';

const JS_MIME_TYPE = 'text/javascript';
const CSS_MIME_TYPE = 'text/css';

/**
 * Util class for work directly with DOM element
 */
@Injectable()
export class DotDOMHtmlUtilService {

    public createLinkElement(href: string): any {
        const cssElement = document.createElement('link');

        cssElement.rel = 'stylesheet';
        cssElement.type = CSS_MIME_TYPE;
        cssElement.media = 'all';
        cssElement.href = href;

        return cssElement;
    }

    public creatExternalScriptElement(src: string, onLoadCallback?: () => void): any {
        const script = this.createScriptElement();
        script.src = src;
        script.onload = onLoadCallback;

        return script;
    }

    public createInlineScriptElement(text: string): any {
        const script = this.createScriptElement();
        script.text = text;

        return script;
    }

    private createScriptElement(): any {
        const script: any = document.createElement('script');
        script.type = JS_MIME_TYPE;

        return script;
    }
}
