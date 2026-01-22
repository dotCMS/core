import { Component, input, output } from '@angular/core';

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    templateUrl: './dot-convert-wysiwyg-to-block.component.html',
    standalone: false,
    host: {
        class: 'mt-6 block border border-gray-300 p-4 rounded-sm'
    }
})
export class DotConvertWysiwygToBlockComponent {
    readonly $currentFieldType = input({ alias: 'currentFieldType' });

    readonly $convert = output<MouseEvent>();

    accept = false;
}
