import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SplitterModule } from 'primeng/splitter';
import { TextareaModule } from 'primeng/textarea';

import { DotAgentThinkingComponent } from '@dotcms/ai-ui';
import { DOT_AI_CHAT_MESSAGE_STATE } from '@dotcms/dotcms-models';
import { DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { DotAiSettingsPanelComponent } from '../../components/dot-ai-settings-panel/dot-ai-settings-panel.component';
import { DotAiStore } from '../../store/dot-ai.store';

/**
 * Chat tab: a conversation over the same indexed content Search reads, streamed token by
 * token and stoppable mid-answer.
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
        DotAgentThinkingComponent,
        DotColorIconComponent,
        DotAiSettingsPanelComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-chat.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiChatComponent {
    protected readonly store = inject(DotAiStore);

    protected readonly states = DOT_AI_CHAT_MESSAGE_STATE;
    protected readonly splitterPt = { root: { class: 'border-0! rounded-none!' } };

    protected readonly $draft = signal('');

    protected readonly thread = viewChild<{ nativeElement: HTMLElement }>('thread');

    protected onSend(): void {
        const prompt = this.$draft().trim();

        if (!prompt || !this.store.isConfigured()) {
            return;
        }

        this.store.sendChat(prompt);
        this.$draft.set('');
        queueMicrotask(() => this.#scrollToBottom());
    }

    /** Enter sends; Shift+Enter inserts a newline (FR-011). */
    protected onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.onSend();
        }
    }

    #scrollToBottom(): void {
        const el = this.thread()?.nativeElement;

        if (el) {
            el.scrollTop = el.scrollHeight;
        }
    }
}
