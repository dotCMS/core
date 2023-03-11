import { Directive, Input, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';

/**
 * Directive that returns in "data" variable the relative date.
 */
@Directive({
    selector: '[dotRelativeDate]',
    standalone: true
})
export class DotRelativeDateDirective implements OnInit {
    private _relativeDate: string;

    constructor(
        private dotFormatDateService: DotFormatDateService,
        private templateRef: TemplateRef<{ data: string }>,
        private viewContainer: ViewContainerRef
    ) {}

    ngOnInit() {
        const viewRef = this.viewContainer.createEmbeddedView(this.templateRef);
        viewRef.context.data = this._relativeDate;
    }

    @Input()
    set dotRelativeDate(date: string) {
        this._relativeDate = this.dotFormatDateService.getRelative(
            new Date(date).getTime().toString(),
            new Date()
        );
    }
}
