import { exposeComponent } from '@hashbrownai/angular';
import { s } from '@hashbrownai/core';
import { marked } from 'marked';

import {
    Component,
    ViewEncapsulation,
    computed,
    inject,
    input,
    SecurityContext
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
    selector: 'app-markdown',
    encapsulation: ViewEncapsulation.None,
    template: `
        <div class="ai-markdown" [innerHTML]="html()"></div>
    `,
    styles: `
        .ai-markdown {
            font-size: 0.875rem;
            line-height: 1.7;
            color: var(--p-text-color);
            word-break: break-word;

            > *:first-child {
                margin-top: 0;
            }

            > *:last-child {
                margin-bottom: 0;
            }

            h1,
            h2,
            h3,
            h4,
            h5,
            h6 {
                font-weight: 700;
                line-height: 1.3;
                margin: 1.25rem 0 0.5rem;
                color: var(--p-text-color);
            }

            h1 {
                font-size: 1.25rem;
            }

            h2 {
                font-size: 1.125rem;
            }

            h3 {
                font-size: 1rem;
            }

            h4,
            h5,
            h6 {
                font-size: 0.875rem;
            }

            p {
                margin: 0 0 0.75rem;
            }

            a {
                color: var(--p-primary-color);
                text-decoration: underline;
                text-underline-offset: 2px;
            }

            ul,
            ol {
                padding-left: 1.5rem;
                margin: 0 0 0.75rem;
            }

            ul {
                list-style-type: disc;
            }

            ol {
                list-style-type: decimal;
            }

            li {
                margin-bottom: 0.35rem;

                > p {
                    margin-bottom: 0.25rem;
                }
            }

            code {
                font-size: 0.8em;
                background-color: var(--p-surface-200);
                padding: 0.15rem 0.4rem;
                border-radius: 4px;
                font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
            }

            pre {
                background-color: var(--p-surface-200);
                padding: 0.75rem 1rem;
                border-radius: 6px;
                overflow-x: auto;
                margin: 0 0 0.75rem;

                code {
                    background-color: transparent;
                    padding: 0;
                    font-size: 0.8125rem;
                    line-height: 1.5;
                }
            }

            blockquote {
                border-left: 3px solid var(--p-primary-color);
                padding: 0.25rem 0 0.25rem 0.75rem;
                margin: 0 0 0.75rem;
                color: var(--p-text-muted-color);

                > p:last-child {
                    margin-bottom: 0;
                }
            }

            strong {
                font-weight: 700;
            }

            hr {
                border: none;
                border-top: 1px solid var(--p-surface-200);
                margin: 1rem 0;
            }

            table {
                width: 100%;
                border-collapse: collapse;
                margin: 0 0 0.75rem;
                font-size: 0.8125rem;

                th,
                td {
                    border: 1px solid var(--p-surface-200);
                    padding: 0.4rem 0.625rem;
                    text-align: left;
                }

                th {
                    font-weight: 700;
                    background-color: var(--p-surface-100);
                }
            }

            img {
                max-width: 100%;
                border-radius: 6px;
            }
        }
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
