import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'dotcms-dot-theme',
    imports: [CommonModule],
    templateUrl: './dot-theme.component.html',
    styleUrl: './dot-theme.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotThemeComponent {}
