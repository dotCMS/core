import { MarkdownModule } from 'ngx-markdown';

import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SplitterModule } from 'primeng/splitter';
import { TextareaModule } from 'primeng/textarea';

import { DotAgentThinkingComponent } from '@dotcms/ai-ui';
import { DOT_AI_ANSWER_STATE } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAiSettingsPanelComponent } from '../../components/dot-ai-settings-panel/dot-ai-settings-panel.component';
import { DotAiStore } from '../../store/dot-ai.store';

/**
 * Chat tab: ask a question of the indexed content and watch the answer stream in.
 *
 * A prompt form, not a conversation. The completions endpoint takes one `prompt` and keeps no
 * history, so each submit is independent and replaces the previous answer. The composer sits
 * above the answer to say that in the layout itself, and the question is left in the textarea
 * so it can be edited and asked again.
 *
 * Answers carry no source list. Only the non-streaming response mode returns
 * `dotCMSResults`, and progressive rendering was chosen over source attribution — so the
 * empty-state copy must not promise sources (spec Out of Scope).
 */
@Component({
    selector: 'dot-ai-chat',
    imports: [
        FormsModule,
        ButtonModule,
        TextareaModule,
        SplitterModule,
        MarkdownModule,
        DotAgentThinkingComponent,
        DotAiSettingsPanelComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-chat.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiChatComponent {
    protected readonly store = inject(DotAiStore);

    protected readonly states = DOT_AI_ANSWER_STATE;
    protected readonly splitterPt = { root: { class: 'border-0! rounded-none!' } };

    protected readonly $draft = signal('');

    protected readonly answer = viewChild<{ nativeElement: HTMLElement }>('answer');

    protected onSend(): void {
        const prompt = this.$draft().trim();

        if (!prompt || !this.store.isConfigured()) {
            return;
        }

        this.store.sendChat(prompt);

        // The draft is deliberately kept: the question is only visible here, and asking a
        // variation of it is the common next step.
        queueMicrotask(() => this.#scrollAnswerToTop());
    }

    /** Enter sends; Shift+Enter inserts a newline (FR-011). */
    protected onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.onSend();
        }
    }

    /** A replaced answer starts at the top, not wherever the last one was scrolled to. */
    #scrollAnswerToTop(): void {
        const el = this.answer()?.nativeElement;

        if (el) {
            el.scrollTop = 0;
        }
    }
}
