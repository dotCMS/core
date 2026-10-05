import { MarkdownComponent } from 'ngx-markdown';

import { DestroyRef, Directive, ElementRef, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil } from '@dotcms/ui';

/** How long the "Copied" state stays on the button. */
const COPIED_RESET_MS = 2000;

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
 * Copy button, and colors JSON. ngx-markdown's own copy button needs clipboard.js, which the app
 * doesn't load, so this does it with {@link DotClipboardUtil} instead.
 */
@Directive({
    selector: 'markdown[dotAppsCodeBlocks]',
    providers: [DotClipboardUtil]
})
export class DotAppsCodeBlocksDirective {
    readonly #host = inject<ElementRef<HTMLElement>>(ElementRef);
    readonly #clipboard = inject(DotClipboardUtil);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #timers = new Set<ReturnType<typeof setTimeout>>();

    constructor() {
        const subscription = inject(MarkdownComponent).ready.subscribe(() => this.enhance());
        inject(DestroyRef).onDestroy(() => {
            subscription.unsubscribe();
            this.#timers.forEach((timer) => clearTimeout(timer));
        });
    }

    /** Wraps every code block that hasn't been upgraded yet. */
    enhance(): void {
        const blocks = this.#host.nativeElement.querySelectorAll<HTMLPreElement>(
            'pre:not([data-dot-code-block])'
        );
        blocks.forEach((pre) => this.wrap(pre));
    }

    private wrap(pre: HTMLPreElement): void {
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

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'dot-code-block__copy';
        button.setAttribute('data-testid', 'code-block-copy');
        this.setButtonState(button, false);
        button.addEventListener('click', () => this.copy(button, text));

        header.append(label, button);
        pre.replaceWith(wrapper);
        wrapper.append(header, pre);
    }

    private async copy(button: HTMLButtonElement, text: string): Promise<void> {
        const copied = await this.#clipboard.copy(text.trim());
        if (!copied) {
            return;
        }

        this.setButtonState(button, true);
        const timer = setTimeout(() => {
            this.#timers.delete(timer);
            this.setButtonState(button, false);
        }, COPIED_RESET_MS);
        this.#timers.add(timer);
    }

    private setButtonState(button: HTMLButtonElement, copied: boolean): void {
        const icon = document.createElement('i');
        icon.className = copied ? 'pi pi-check' : 'pi pi-copy';
        const label = document.createElement('span');
        label.textContent = this.#dotMessageService.get(
            copied ? 'apps.code.block.copied' : 'apps.code.block.copy'
        );

        button.classList.toggle('dot-code-block__copy--copied', copied);
        button.replaceChildren(icon, label);
    }
}
