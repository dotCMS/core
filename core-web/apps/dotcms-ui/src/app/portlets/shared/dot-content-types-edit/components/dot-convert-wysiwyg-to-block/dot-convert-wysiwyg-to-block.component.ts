import { CommonModule } from '@angular/common';
import { Component, Input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    templateUrl: './dot-convert-wysiwyg-to-block.component.html',
    standalone: true,
    host: {
        class: 'mt-6 block border border-gray-300 p-4 rounded-sm'
    },
    imports: [CommonModule, FormsModule, ButtonModule, CheckboxModule, DotMessagePipe]
})
export class DotConvertWysiwygToBlockComponent {
    @Input() currentFieldType: { clazz?: string };

    readonly $convert = output<MouseEvent>();

    accept = false;
}
