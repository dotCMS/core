import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-dot-page-tools-seo',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-page-tools-seo.component.html',
    styleUrls: ['./dot-page-tools-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageToolsSeoComponent {}
