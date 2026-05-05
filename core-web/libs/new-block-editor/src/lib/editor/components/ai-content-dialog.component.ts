import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    SecurityContext,
    signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { ButtonDirective } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { Skeleton } from 'primeng/skeleton';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

type Status = 'idle' | 'loading' | 'success' | 'error';

/**
 * Appended to every user prompt so the AI consistently returns HTML the editor
 * can parse cleanly via `commands.insertContent(html)`. Avoids markdown, code
 * fences, and document-wrapper tags that TipTap's schema can't handle.
 */
const AI_HTML_GUARDRAIL =
    'Format the output as semantic HTML. Use only these tags: p, h1, h2, h3, h4, h5, h6, ul, ol, li, strong, em, a, code, pre, blockquote, br. Do not wrap the response in markdown code fences. Do not include <html>, <body>, or <script> tags. Do not insert empty paragraphs (<p></p>) or empty tags between blocks for spacing — natural margins between block elements handle visual separation. Do not use <br> tags as line breaks between blocks or inside empty/near-empty paragraphs (no <p><br></p>); use <br> only for an intentional in-line break inside a paragraph that already has surrounding text.';

/**
 * Centered AI Content modal — opened via {@link EditorModalService.openAiContent}, which
 * wires this component into PrimeNG's {@link DialogService} so it follows the same
 * lifecycle as the other modal pickers (AI image, image picker, video picker).
 *
 * Flow:
 * - Cancel / Discard / Escape / X → close with no value (no insertion).
 * - Insert → close with the generated HTML; the caller in `EditorModalService` subscribes
 *   to `onClose` and runs `editor.chain().focus().insertAINode(html).run()`.
 */
@Component({
    selector: 'dot-ai-content-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, ButtonDirective, Skeleton, DotMessagePipe],
    template: `
        <div class="flex flex-col gap-4">
            <!-- Prompt input -->
            <div class="flex flex-col gap-1">
                <label for="ai-prompt" class="text-xs font-medium text-gray-700">
                    {{ 'dot.block.editor.dialog.ai-content.field.prompt.label' | dm }}
                </label>
                <textarea
                    id="ai-prompt"
                    [formControl]="promptControl"
                    rows="3"
                    [placeholder]="
                        'dot.block.editor.dialog.ai-content.field.prompt.placeholder' | dm
                    "
                    class="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                    (keydown.enter)="onEnterKey($event)"></textarea>
            </div>

            <!-- Preview area -->
            <div
                class="min-h-32 rounded border border-gray-200 bg-gray-50 p-3 text-sm text-gray-800">
                @switch (status()) {
                    @case ('idle') {
                        <p class="m-0 italic text-gray-400">
                            {{ 'dot.block.editor.dialog.ai-content.empty-state' | dm }}
                        </p>
                    }
                    @case ('loading') {
                        <div class="flex flex-col gap-2">
                            <p-skeleton width="100%" height="0.75rem" />
                            <p-skeleton width="92%" height="0.75rem" />
                            <p-skeleton width="78%" height="0.75rem" />
                            <p-skeleton width="65%" height="0.75rem" />
                        </div>
                    }
                    @case ('success') {
                        <div
                            class="prose prose-sm max-w-none text-gray-900"
                            [innerHTML]="result()"></div>
                    }
                    @case ('error') {
                        <p class="m-0 text-red-600">{{ errorMessage() }}</p>
                    }
                }
            </div>
        </div>

        <div class="mt-4 flex w-full items-center justify-end gap-2">
            @if (status() === 'success') {
                <button
                    pButton
                    type="button"
                    severity="secondary"
                    [text]="true"
                    [label]="'dot.block.editor.dialog.ai-content.discard' | dm"
                    (click)="cancel()"></button>
                <button
                    pButton
                    type="button"
                    severity="secondary"
                    [label]="'block-editor.common.regenerate' | dm"
                    (click)="generate()"></button>
                <button pButton type="button" [label]="'Insert' | dm" (click)="insert()"></button>
            } @else {
                <button
                    pButton
                    type="button"
                    severity="secondary"
                    [text]="true"
                    [label]="'Cancel' | dm"
                    (click)="cancel()"></button>
                <button
                    pButton
                    type="button"
                    [label]="'block-editor.common.generate' | dm"
                    [loading]="status() === 'loading'"
                    [disabled]="generateDisabled()"
                    (click)="generate()"></button>
            }
        </div>
    `
})
export class AiContentDialogComponent {
    private readonly dialogRef = inject(DynamicDialogRef);
    private readonly dotAi = inject(DotAiService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly sanitizer = inject(DomSanitizer);

    protected readonly promptControl = new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required]
    });
    protected readonly status = signal<Status>('idle');
    protected readonly result = signal<string>('');
    protected readonly errorMessage = signal<string>('');

    /**
     * `FormControl.invalid` is a plain getter — not a signal — so a `computed` that reads
     * it never re-runs when the user types. Mirroring the control's status into a signal
     * via {@link toSignal} gives the disabled computed something it can actually track.
     */
    private readonly promptStatus = toSignal(this.promptControl.statusChanges, {
        initialValue: this.promptControl.status
    });

    protected readonly generateDisabled = computed(
        () => this.status() === 'loading' || this.promptStatus() !== 'VALID'
    );

    protected onEnterKey(event: Event): void {
        const ke = event as KeyboardEvent;
        if (ke.shiftKey) return; // allow newlines with Shift+Enter
        ke.preventDefault();
        if (!this.generateDisabled()) this.generate();
    }

    protected cancel(): void {
        this.dialogRef.close();
    }

    protected generate(): void {
        const userPrompt = this.promptControl.value.trim();
        if (!userPrompt) return;
        this.status.set('loading');
        this.errorMessage.set('');
        // Append the HTML format directive so the AI consistently returns markup
        // that the editor's `insertContent` can parse cleanly.
        const prompt = `${userPrompt}\n\n${AI_HTML_GUARDRAIL}`;
        this.dotAi.generateContent(prompt).subscribe({
            next: (content) => {
                // Sanitize once — preview ([innerHTML]) and insert (dialogRef.close)
                // both consume `result()`, so they stay byte-for-byte identical.
                const safe = this.sanitizer.sanitize(SecurityContext.HTML, content) ?? '';
                this.result.set(this.stripAiArtifacts(safe));
                this.status.set('success');
            },
            error: (err) => {
                this.errorMessage.set(
                    typeof err === 'string'
                        ? err
                        : this.dotMessageService.get('dot.block.editor.dialog.ai-content.error')
                );
                this.status.set('error');
            }
        });
    }

    /**
     * Removes empty paragraphs and inter-tag whitespace that AIs occasionally emit
     * despite the guardrail prompt.
     *
     * The sanitizer encodes literal newlines as `&#10;` entities — JS `\s` doesn't
     * match those sequences, so the whitespace pattern lists each entity explicitly.
     */
    private stripAiArtifacts(html: string): string {
        const WS = '(?:\\s|&#10;|&#13;|&#9;|&#32;|&#160;|&nbsp;)';

        return html
            .replace(new RegExp(`<p>(?:${WS}|<br\\s*/?>)*</p>`, 'gi'), '')
            .replace(new RegExp(`>${WS}+<`, 'g'), '><');
    }

    protected insert(): void {
        const html = this.result();
        if (!html) return;
        this.dialogRef.close(html);
    }
}
