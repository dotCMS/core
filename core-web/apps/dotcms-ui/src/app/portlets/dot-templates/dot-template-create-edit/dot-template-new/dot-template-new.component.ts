import { Component, OnInit } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { take } from 'rxjs/operators';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { BINARY_OPTION, DotBinaryOptionSelectorComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-dot-template-new',
    templateUrl: './dot-template-new.component.html',
    styleUrls: ['./dot-template-new.component.scss']
})
export class DotTemplateNewComponent implements OnInit {
    private readonly options: BINARY_OPTION = {
        option1: {
            value: 'designer',
            message: 'templates.template.selector.design',
            icon: 'web',
            label: 'templates.template.selector.label.designer'
        },
        option2: {
            value: 'advanced',
            message: 'templates.template.selector.advanced',
            icon: 'settings_applications',
            label: 'templates.template.selector.label.advanced'
        }
    };

    constructor(
        private dialogService: DialogService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit(): void {
        const ref = this.dialogService.open(DotBinaryOptionSelectorComponent, {
            header: this.dotMessageService.get('templates.select.template.title'),
            width: '37rem',
            data: { options: this.options },
            contentStyle: { padding: '0px' }
        });

        ref.onClose.pipe(take(1)).subscribe((value) => {
            value
                ? this.dotRouterService.gotoPortlet(`/templates/new/${value}`)
                : this.dotRouterService.goToURL(`/templates`);
        });
    }
}
