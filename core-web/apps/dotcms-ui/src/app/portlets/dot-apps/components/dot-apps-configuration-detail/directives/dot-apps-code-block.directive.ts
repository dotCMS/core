import { MarkdownComponent } from 'ngx-markdown';

import {
    ApplicationRef,
    ComponentRef,
    createComponent,
    DestroyRef,
    Directive,
    ElementRef,
    EnvironmentInjector,
    inject
} from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCopyButtonComponent } from '@dotcms/ui';

const JSON_TOKEN =
    /("(?:\\.|[^"\\])*")(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g;

const escapeHtml = (text: string): string =>
    text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

/**
 * Wraps JSON tokens in spans so the code block can color keys, strings, numbers and literals.
 * Every piece of the text is HTML-escaped first.
 *
 * @param code the raw JSON text
 */
export function highlightJson(code: string): string {
    let html = '';
    let last = 0;

    for (const match of code.matchAll(JSON_TOKEN)) {
        const [token, string, colon, literal] = match;
        html += escapeHtml(code.slice(last, match.index ?? 0));

        if (string !== undefined) {
            const kind = colon ? 'key' : 'string';
            html += `<span class="dot-code-block__${kind}">${escapeHtml(string)}</span>`;
            html += colon ? escapeHtml(colon) : '';
        } else {
            const kind = literal ? 'literal' : 'number';
            html += `<span class="dot-code-block__${kind}">${escapeHtml(token)}</span>`;
        }

        last = (match.index ?? 0) + token.length;
    }

    return html + escapeHtml(code.slice(last));
}

/**
 * Upgrades the code blocks of a rendered `<markdown>` hint: adds a header with the language and a
 * {@link DotCopyButtonComponent}, and colors JSON. ngx-markdown's own copy button needs
 * clipboard.js, which the app doesn't load. The markdown is rendered outside Angular's templates,
 * so the copy buttons are created and attached to the application by hand.
 */
@Directive({
    selector: 'markdown[dotAppsCodeBlocks]'
})
export class DotAppsCodeBlocksDirective {
    readonly #host = inject<ElementRef<HTMLElement>>(ElementRef);
    readonly #appRef = inject(ApplicationRef);
    readonly #environmentInjector = inject(EnvironmentInjector);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #copyButtons: ComponentRef<DotCopyButtonComponent>[] = [];

    constructor() {
        const subscription = inject(MarkdownComponent).ready.subscribe(() => this.enhance());
        inject(DestroyRef).onDestroy(() => {
            subscription.unsubscribe();
            this.#copyButtons.forEach((ref) => {
                this.#appRef.detachView(ref.hostView);
                ref.destroy();
            });
        });
    }

    /** Wraps every code block that hasn't been upgraded yet. */
    enhance(): void {
        const blocks = this.#host.nativeElement.querySelectorAll<HTMLPreElement>(
            'pre:not([data-dot-code-block])'
        );
        blocks.forEach((pre) => this.#wrap(pre));
    }

    #wrap(pre: HTMLPreElement): void {
        const code = pre.querySelector('code');
        const text = (code ?? pre).textContent ?? '';
        const language = /language-(\w+)/.exec(code?.className ?? '')?.[1] ?? '';

        pre.setAttribute('data-dot-code-block', '');
        if (code && language === 'json') {
            code.innerHTML = highlightJson(text);
        }

        const wrapper = document.createElement('div');
        wrapper.className = 'dot-code-block';

        const header = document.createElement('div');
        header.className = 'dot-code-block__header';

        const label = document.createElement('span');
        label.className = 'dot-code-block__language';
        label.textContent = language || 'code';

        header.append(label, this.#createCopyButton(text.trim()));
        pre.replaceWith(wrapper);
        wrapper.append(header, pre);
    }

    #createCopyButton(text: string): HTMLElement {
        const ref = createComponent(DotCopyButtonComponent, {
            environmentInjector: this.#environmentInjector
        });
        ref.setInput('copy', text);
        ref.setInput('label', this.#dotMessageService.get('apps.code.block.copy'));
        this.#appRef.attachView(ref.hostView);
        ref.changeDetectorRef.detectChanges();
        this.#copyButtons.push(ref);

        return ref.location.nativeElement;
    }
}
