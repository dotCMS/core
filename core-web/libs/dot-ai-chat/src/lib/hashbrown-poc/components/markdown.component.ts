import { exposeComponent } from '@hashbrownai/angular';
import { s } from '@hashbrownai/core';
import { marked } from 'marked';

import { Component, computed, inject, input, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
    selector: 'app-markdown',
    template: `
        <div class="prose prose-sm max-w-none" [innerHTML]="html()"></div>
    `
})
export class MarkdownComponent {
    private readonly sanitizer = inject(DomSanitizer);

    readonly data = input.required<string>();

    readonly html = computed(() => {
        const parsed = marked.parse(this.data(), { async: false }) as string;
        return this.sanitizer.sanitize(SecurityContext.HTML, parsed) ?? '';
    });
}

export const AiMarkdownComponent = exposeComponent(MarkdownComponent, {
    description: 'Show markdown to the user',
    input: {
        data: s.streaming.string('The markdown content')
    }
});
