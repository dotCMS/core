import { MarkdownModule } from 'ngx-markdown';

import { Component, inject, signal, viewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotAgentThinkingComponent, DotAiPromptInputComponent } from '@dotcms/ai-ui';
import { DOT_AI_ANSWER_STATE } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAiWorkspaceComponent } from '../../components/dot-ai-workspace/dot-ai-workspace.component';
import { DotAiStore } from '../../store/dot-ai.store';

/**
 * Chat tab: ask a question of the indexed content and watch the answer stream in.
 *
 * A prompt form, not a conversation. The completions endpoint takes one `prompt` and keeps no
 * history, so each submit is independent and **replaces** the previous answer — there is no
 * transcript, which is what keeps the screen from implying a memory it does not have. The
 * composer sits below the answer, the usual arrangement for this kind of screen, and the
 * question is left in the textarea so it can be edited and asked again.
 *
 * Answers carry no source list. Only the non-streaming response mode returns
 * `dotCMSResults`, and progressive rendering was chosen over source attribution — so the
 * empty-state copy must not promise sources (spec Out of Scope).
 */
@Component({
    selector: 'dot-ai-chat',
    imports: [
        ButtonModule,
        MarkdownModule,
        DotAgentThinkingComponent,
        DotAiPromptInputComponent,
        DotAiWorkspaceComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-chat.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiChatComponent {
    protected readonly store = inject(DotAiStore);

    protected readonly states = DOT_AI_ANSWER_STATE;

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

    /** A replaced answer starts at the top, not wherever the last one was scrolled to. */
    #scrollAnswerToTop(): void {
        const el = this.answer()?.nativeElement;

        if (el) {
            el.scrollTop = 0;
        }
    }
}
