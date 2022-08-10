import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    templateUrl: './dot-convert-wysiwyg-to-block.component.html',
    styleUrls: ['./dot-convert-wysiwyg-to-block.component.scss']
})
export class DotConvertWysiwygToBlockComponent {
    @Input() currentFieldType;

    accept = false;
}
