import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'lib-dot-insights-shell',
    imports: [CommonModule],
    templateUrl: './dot-insights-shell.component.html',
    styleUrl: './dot-insights-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotInsightsShellComponent {}
