import { RenderMessageComponent, uiChatResource } from '@hashbrownai/angular';

import { Component, model } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { AiMarkdownComponent } from './components/markdown.component';
import { HASHBROWN_CHAT_SYSTEM_PROMPT } from './hashbrown-chat.prompt';
import { getCategoriesTool } from './tools/get-categories.tool';
import { getProductsTool } from './tools/get-products.tool';

@Component({
    selector: 'dotcms-hashbrown-chat',
    imports: [FormsModule, InputTextModule, ButtonModule, RenderMessageComponent],
    templateUrl: './hashbrown-chat.component.html'
})
export class HashbrownChatComponent {
    readonly chat = uiChatResource({
        model: 'gpt-5.4',
        system: HASHBROWN_CHAT_SYSTEM_PROMPT,
        components: [AiMarkdownComponent],
        tools: [getProductsTool, getCategoriesTool]
    });

    readonly userMessage = model<string>('');

    send(): void {
        const message = this.userMessage().trim();
        if (!message) {
            return;
        }

        this.chat.sendMessage({ role: 'user', content: message });
        this.userMessage.set('');
    }
}
