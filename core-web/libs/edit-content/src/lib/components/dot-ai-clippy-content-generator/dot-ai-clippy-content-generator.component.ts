import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { OverlayPanelModule } from 'primeng/overlaypanel';

@Component({
    selector: 'dot-ai-clippy-content-generator',
    standalone: true,
    imports: [CommonModule, InputTextareaModule, OverlayPanelModule, ButtonModule],
    templateUrl: './dot-ai-clippy-content-generator.component.html',
    styleUrl: './dot-ai-clippy-content-generator.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAiClippyContentGeneratorComponent {
    protected readonly messages = [
        { from: 'Chat', message: "Hello! I'm the dotAI CLIPPY Chat, how can I help you today?" }
    ];
}
