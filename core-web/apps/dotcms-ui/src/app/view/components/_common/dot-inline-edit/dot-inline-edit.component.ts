import { Component, Input, TemplateRef, ViewChild } from '@angular/core';

import { Inplace } from 'primeng/inplace';

@Component({
    selector: 'dot-inline-edit',
    templateUrl: './dot-inline-edit.component.html',
    standalone: false
})
export class DotInlineEditComponent {
    @Input()
    inlineEditDisplayTemplate: TemplateRef<unknown>;
    @Input()
    inlineEditContentTemplate: TemplateRef<unknown>;

    @ViewChild('contentTypeInlineEdit') contentTypeInlineEdit: Inplace;

    /**
     * Manually hides the content/edit section of p-inplace
     *
     * @memberof DotInlineEditComponent
     */
    hideContent(): void {
        this.contentTypeInlineEdit.deactivate();
    }
}
