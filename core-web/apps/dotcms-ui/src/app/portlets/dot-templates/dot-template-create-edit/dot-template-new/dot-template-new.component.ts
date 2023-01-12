import { Component, OnInit } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { take } from 'rxjs/operators';


import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotMessageService } from '@dotcms/data-access';
import { DotTemplateSelectorComponent } from '@portlets/dot-templates/dot-template-list/components/dot-template-selector/dot-template-selector.component';

@Component({
    selector: 'dot-dot-template-new',
    templateUrl: './dot-template-new.component.html',
    styleUrls: ['./dot-template-new.component.scss']
})
export class DotTemplateNewComponent implements OnInit {
    constructor(
        private dialogService: DialogService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit(): void {
        const ref = this.dialogService.open(DotTemplateSelectorComponent, {
            header: this.dotMessageService.get('templates.select.template.title'),
            width: '37rem'
        });

        ref.onClose.pipe(take(1)).subscribe((value: string) => {
            value
                ? this.dotRouterService.gotoPortlet(`/templates/new/${value}`)
                : this.dotRouterService.goToURL(`/templates`);
        });
    }
}
