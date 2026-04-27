import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonDirective } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { Skeleton } from 'primeng/skeleton';

import { Editor } from '@tiptap/core';

import { DotAiService } from '../services/dot-ai.service';
import { EditorDialogManagerService } from '../services/editor-dialog-manager.service';

type Status = 'idle' | 'loading' | 'success' | 'error';

@Component({
    selector: 'dot-ai-content-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DialogModule, ReactiveFormsModule, ButtonDirective, Skeleton],
    template: `
        <p-dialog
            [visible]="manager.aiContentOpen()"
            (visibleChange)="onVisibleChange($event)"
            [modal]="true"
            [closable]="true"
            [closeOnEscape]="true"
            [draggable]="false"
            [dismissableMask]="false"
            header="Ask AI"
            [style]="{ width: '720px', maxWidth: '90vw' }">
            <div class="flex flex-col gap-4">
                <!-- Prompt input -->
                <div class="flex flex-col gap-1">
                    <label for="ai-prompt" class="text-xs font-medium text-gray-700">Prompt</label>
                    <textarea
                        id="ai-prompt"
                        [formControl]="promptControl"
                        rows="3"
                        placeholder="Ask AI to generate text — e.g. 'Write a short paragraph about the benefits of remote work'"
                        class="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                        (keydown.enter)="onEnterKey($event)"></textarea>
                </div>

                <!-- Preview area -->
                <div
                    class="min-h-32 rounded border border-gray-200 bg-gray-50 p-3 text-sm text-gray-800">
                    @switch (status()) {
                        @case ('idle') {
                            <p class="m-0 italic text-gray-400">
                                The generated text will appear here.
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

            <ng-template pTemplate="footer">
                <div class="flex w-full items-center justify-end gap-2">
                    @if (status() === 'success') {
                        <button
                            pButton
                            type="button"
                            severity="secondary"
                            [text]="true"
                            label="Discard"
                            (click)="discard()"></button>
                        <button
                            pButton
                            type="button"
                            severity="secondary"
                            label="Regenerate"
                            (click)="generate()"></button>
                        <button pButton type="button" label="Insert" (click)="insert()"></button>
                    } @else {
                        <button
                            pButton
                            type="button"
                            severity="secondary"
                            [text]="true"
                            label="Cancel"
                            (click)="close()"></button>
                        <button
                            pButton
                            type="button"
                            label="Generate"
                            [loading]="status() === 'loading'"
                            [disabled]="generateDisabled()"
                            (click)="generate()"></button>
                    }
                </div>
            </ng-template>
        </p-dialog>
    `
})
export class AiContentDialogComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorDialogManagerService);
    private readonly dotAi = inject(DotAiService);

    protected readonly promptControl = new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required]
    });
    protected readonly status = signal<Status>('idle');
    protected readonly result = signal<string>('');
    protected readonly errorMessage = signal<string>('');

    protected readonly generateDisabled = computed(
        () => this.status() === 'loading' || this.promptControl.invalid
    );

    constructor() {
        // Reset local state every time the dialog reopens.
        effect(() => {
            if (this.manager.aiContentOpen()) {
                this.promptControl.reset('');
                this.status.set('idle');
                this.result.set('');
                this.errorMessage.set('');
            }
        });
    }

    protected onVisibleChange(visible: boolean): void {
        if (!visible) this.manager.closeAiContent();
    }

    protected onEnterKey(event: Event): void {
        const ke = event as KeyboardEvent;
        if (ke.shiftKey) return; // allow newlines with Shift+Enter
        ke.preventDefault();
        if (!this.generateDisabled()) this.generate();
    }

    protected close(): void {
        this.manager.closeAiContent();
    }

    protected discard(): void {
        this.manager.closeAiContent();
    }

    protected generate(): void {
        const prompt = this.promptControl.value.trim();
        if (!prompt) return;
        this.status.set('loading');
        this.errorMessage.set('');
        this.dotAi.generateContent(prompt).subscribe({
            next: (content) => {
                this.result.set(content);
                this.status.set('success');
            },
            error: (err) => {
                this.errorMessage.set(typeof err === 'string' ? err : 'Generation failed');
                this.status.set('error');
            }
        });
    }

    protected insert(): void {
        const html = this.result();
        if (!html) return;
        this.editor().chain().focus().insertAINode(html).run();
        this.manager.closeAiContent();
    }
}
