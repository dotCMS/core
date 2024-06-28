import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-locale-variables',
    standalone: true,
    imports: [CommonModule, TableModule, DotMessagePipe],
    templateUrl: './dot-locale-variables.component.html',
    styleUrl: './dot-locale-variables.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocaleVariablesComponent {}
