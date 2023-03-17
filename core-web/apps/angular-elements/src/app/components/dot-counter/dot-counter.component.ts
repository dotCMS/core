import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dotcms-dot-counter',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-counter.component.html',
    styleUrls: ['./dot-counter.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCounterComponent {}
