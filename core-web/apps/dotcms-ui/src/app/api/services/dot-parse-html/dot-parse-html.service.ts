import { Injectable, Renderer2, RendererFactory2, inject } from '@angular/core';

/**
 * Parse html and execute scripts
 *
 * @export
 * @class DotParseHtmlService
 */
@Injectable()
export class DotParseHtmlService {
    private renderer: Renderer2;

    constructor() {
        const rendererFactory = inject(RendererFactory2);

        this.renderer = rendererFactory.createRenderer(null, null);
    }

    /**
     * Insert in an element the given code including the execution of scripts.
     *
     * @param {string} code
     * @param {HTMLElement} element
     * @param {boolean} clear
     * @memberof DotParseHtmlService
     */
    parse(code: string, element: HTMLElement, clear: boolean): void {
        if (clear) {
            this.clearElement(element);
        }

        const placeholder = document.createElement('div');
        placeholder.innerHTML = code;
        Array.from(placeholder.childNodes).forEach((el: HTMLElement) => {
            const parsedEl = this.isScriptElement(el.tagName)
                ? this.createScriptEl(el.innerHTML)
                : el;
            this.renderer.appendChild(element, parsedEl);
        });
    }

    private isScriptElement(tag: string): boolean {
        return tag === 'SCRIPT';
    }

    private createScriptEl(content: string): HTMLScriptElement {
        const script = this.renderer.createElement('script');
        this.renderer.setAttribute(script, 'type', 'text/javascript');
        const text = this.renderer.createText(content);
        this.renderer.appendChild(script, text);

        return script;
    }

    private clearElement(element: HTMLElement): void {
        Array.from(element.childNodes).forEach((child: HTMLElement) => {
            this.renderer.removeChild(element, child);
        });
    }
}
