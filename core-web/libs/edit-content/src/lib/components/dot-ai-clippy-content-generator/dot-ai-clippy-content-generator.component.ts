import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, input, model, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { OverlayPanelModule } from 'primeng/overlaypanel';

@Component({
    selector: 'dot-ai-clippy-content-generator',
    standalone: true,
    imports: [CommonModule, InputTextareaModule, OverlayPanelModule, ButtonModule, FormsModule],
    templateUrl: './dot-ai-clippy-content-generator.component.html',
    styleUrl: './dot-ai-clippy-content-generator.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAiClippyContentGeneratorComponent {
    protected readonly messages = [
        { from: 'Chat', message: "Hello! I'm the dotAI CLIPPY Chat, how can I help you today?" }
    ];

    message = model<string>('');

    isClippyThinking = input<boolean>(false);

    @Output() generateContent = new EventEmitter<string>();

    emitGenerateContent() {
        this.generateContent.emit(this.message());
        this.message.set('');
    }
}
