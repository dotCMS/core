import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-dot-uve-mode-selector',
    imports: [CommonModule],
    templateUrl: './dot-uve-mode-selector.component.html',
    styleUrl: './dot-uve-mode-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveModeSelectorComponent {}
