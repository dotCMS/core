import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DialogModule } from 'primeng/dialog';
import { InputTextareaModule } from 'primeng/inputtextarea';

@Component({
    selector: 'dot-ai-clippy-content-generator',
    standalone: true,
    imports: [CommonModule, InputTextareaModule, DialogModule],
    templateUrl: './dot-ai-clippy-content-generator.component.html',
    styleUrl: './dot-ai-clippy-content-generator.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAiClippyContentGeneratorComponent {
    protected readonly messages = [
        { from: 'Chat', message: "Hello! I'm the dotAI CLIPPY Chat, how can I help you today?" }
    ];
}
