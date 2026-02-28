import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-convert-to-block-info',
    templateUrl: './dot-convert-to-block-info.component.html',
    standalone: true,
    host: {
        class: 'flex justify-center items-center gap-1 px-6 py-2 bg-primary-100/50 text-primary-900 rounded-sm'
    },
    imports: [CommonModule, ButtonModule, DotMessagePipe]
})
export class DotConvertToBlockInfoComponent {
    readonly $currentFieldType = input({ alias: 'currentFieldType' });
    readonly $action = output<MouseEvent>();
    readonly $currentField = input({ alias: 'currentField' });
}
