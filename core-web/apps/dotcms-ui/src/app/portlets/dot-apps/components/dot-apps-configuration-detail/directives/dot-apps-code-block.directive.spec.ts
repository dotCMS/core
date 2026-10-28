import { createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/vitest';
import { MarkdownComponent } from 'ngx-markdown';

import { Component, EventEmitter, forwardRef, Output } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil } from '@dotcms/ui';

import { DotAppsCodeBlocksDirective, highlightJson } from './dot-apps-code-block.directive';

/** Stands in for ngx-markdown's component: only its `ready` output is used. */
@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'markdown',
    template: '<ng-content />',
    providers: [
        { provide: MarkdownComponent, useExisting: forwardRef(() => MarkdownStubComponent) }
    ]
})
class MarkdownStubComponent {
    @Output() ready = new EventEmitter<void>();
}

const JSON_BLOCK =
    '<pre><code class="language-json">{"url": "https://a.com", "on": true, "n": 3}</code></pre>';

describe('DotAppsCodeBlocksDirective', () => {
    let spectator: SpectatorHost<DotAppsCodeBlocksDirective>;
    let copy: ReturnType<typeof vi.spyOn>;

    const createHost = createHostFactory({
        component: DotAppsCodeBlocksDirective,
        imports: [MarkdownStubComponent],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    const markdownStub = () =>
        spectator.debugElement.injector.get(MarkdownComponent) as unknown as MarkdownStubComponent;

    const render = (html: string) => {
        spectator = createHost('<markdown dotAppsCodeBlocks></markdown>');
        const markdown = spectator.hostElement.querySelector('markdown');
        if (markdown) {
            markdown.innerHTML = html;
        }
        markdownStub().ready.emit();
    };

    beforeEach(() => {
        copy = vi.spyOn(DotClipboardUtil.prototype, 'copy').mockResolvedValue(true);
    });

    afterEach(() => copy.mockRestore());

    it('should wrap code blocks with a language label and a copy button', () => {
        render(JSON_BLOCK);

        expect(spectator.element.querySelector('.dot-code-block pre')).toBeTruthy();
        expect(spectator.element.querySelector('.dot-code-block__language')?.textContent).toBe(
            'json'
        );
        const copyButton = spectator.element.querySelector(
            '.dot-code-block__header dot-copy-button [data-testid="copy-to-clipboard"]'
        );
        expect(copyButton?.textContent).toContain('apps.code.block.copy');
    });

    it('should color JSON keys and values', () => {
        render(JSON_BLOCK);

        expect(spectator.element.querySelector('.dot-code-block__key')?.textContent).toBe('"url"');
        expect(spectator.element.querySelector('.dot-code-block__string')?.textContent).toBe(
            '"https://a.com"'
        );
        expect(spectator.element.querySelector('.dot-code-block__literal')?.textContent).toBe(
            'true'
        );
        expect(spectator.element.querySelector('.dot-code-block__number')?.textContent).toBe('3');
    });

    it('should copy the raw code with the shared copy button', async () => {
        render(JSON_BLOCK);

        spectator.click(
            spectator.element.querySelector<HTMLElement>(
                '[data-testid="copy-to-clipboard"] button'
            ) ?? undefined
        );
        await new Promise((resolve) => setTimeout(resolve));

        expect(copy).toHaveBeenCalledWith('{"url": "https://a.com", "on": true, "n": 3}');
    });

    it('should create one copy button per code block', () => {
        render(JSON_BLOCK + '<pre><code>plain text</code></pre>');

        expect(spectator.element.querySelectorAll('dot-copy-button').length).toBe(2);
        expect(
            Array.from(spectator.element.querySelectorAll('.dot-code-block__language')).map(
                (label) => label.textContent
            )
        ).toEqual(['json', 'code']);
    });

    it('should not wrap the same block twice', () => {
        render(JSON_BLOCK);
        markdownStub().ready.emit();

        expect(Array.from(spectator.element.querySelectorAll('.dot-code-block')).length).toBe(1);
    });

    it('highlightJson should escape HTML', () => {
        expect(highlightJson('{"a": "<b>"}')).not.toContain('<b>');
    });
});
