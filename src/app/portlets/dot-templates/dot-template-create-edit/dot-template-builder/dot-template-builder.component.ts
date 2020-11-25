import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DotTemplateItem } from '../store/dot-template.store';

@Component({
    selector: 'dot-template-builder',
    templateUrl: './dot-template-builder.component.html',
    styleUrls: ['./dot-template-builder.component.scss']
})
export class DotTemplateBuilderComponent {
    @Input() item: DotTemplateItem;
    @Output() save = new EventEmitter<DotTemplateItem>();
    @Output() cancel = new EventEmitter();

    constructor() {}
}
