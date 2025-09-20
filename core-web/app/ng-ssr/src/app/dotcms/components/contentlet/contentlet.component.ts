import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotCMSBasicContentlet } from '@dotcms/types';

@Component({
    selector: 'app-contentlet',
    imports: [CommonModule],
    templateUrl: './contentlet.component.html',
    styleUrl: './contentlet.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentletComponent {
    contentlet = input.required<DotCMSBasicContentlet>();
}
