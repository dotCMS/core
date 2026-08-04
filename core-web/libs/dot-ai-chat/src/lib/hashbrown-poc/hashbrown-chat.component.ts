import { RenderMessageComponent, uiChatResource } from '@hashbrownai/angular';

import { Component, DestroyRef, computed, inject, model } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { AiContentTypeCardComponent } from './components/content-type-card.component';
import { AiContentTypeListComponent } from './components/content-type-list.component';
import { AiFavoritePageCardComponent } from './components/favorite-page-card.component';
import { AiFavoritePageListComponent } from './components/favorite-page-list.component';
import { AiMarkdownComponent } from './components/markdown.component';
import { HASHBROWN_CHAT_SYSTEM_PROMPT } from './hashbrown-chat.prompt';
import { HashbrownChatBridgeService } from './services/hashbrown-chat-bridge.service';
import { getContentTypesTool } from './tools/get-content-types.tool';
import { getFavoritePagesTool } from './tools/get-favorite-pages.tool';
import { searchDocumentationTool } from './tools/search-documentation.tool';

@Component({
    selector: 'dotcms-hashbrown-chat',
    imports: [FormsModule, InputTextModule, ButtonModule, RenderMessageComponent],
    templateUrl: './hashbrown-chat.component.html'
})
export class HashbrownChatComponent {
    private readonly destroyRef = inject(DestroyRef);
    private readonly hashbrownChatBridgeService = inject(HashbrownChatBridgeService);

    readonly chat = uiChatResource({
        model: 'gpt-5.4',
        system: HASHBROWN_CHAT_SYSTEM_PROMPT,
        components: [
            AiMarkdownComponent,
            AiContentTypeListComponent,
            AiContentTypeCardComponent,
            AiFavoritePageListComponent,
            AiFavoritePageCardComponent
        ],
        tools: [getContentTypesTool, getFavoritePagesTool, searchDocumentationTool]
    });

    readonly userMessage = model<string>('');

    constructor() {
        this.hashbrownChatBridgeService.userMessage$
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((message: string) => {
                this.chat.sendMessage({ role: 'user', content: message });
            });
    }

    messagesWithContentTypes = computed(() => {
        const messages = this.chat.value();
        return messages.filter(
            (message) =>
                (message.role === 'assistant' || message.role === 'user') && message.content
        );
    });

    send(): void {
        const message = this.userMessage().trim();
        if (!message) {
            return;
        }

        this.chat.sendMessage({ role: 'user', content: message });
        this.userMessage.set('');
    }
}
