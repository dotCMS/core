import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';

import { DotMessagePipe } from '@dotcms/ui';

import { FieldType } from '../fields/models';

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    templateUrl: './dot-convert-wysiwyg-to-block.component.html',
    standalone: true,
    host: {
        class: 'mt-6 block border border-gray-300 p-4 rounded-sm'
    },
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule, ButtonModule, CheckboxModule, DotMessagePipe]
})
export class DotConvertWysiwygToBlockComponent {
    // See DotConvertToBlockInfoComponent: `input()` takes the initial value first, so
    // `input({ alias })` left this input unaliased and `[currentFieldType]` bound to nothing.
    readonly $currentFieldType = input<FieldType | undefined>(undefined, {
        alias: 'currentFieldType'
    });

    readonly $convert = output<MouseEvent>();

    accept = false;
}
