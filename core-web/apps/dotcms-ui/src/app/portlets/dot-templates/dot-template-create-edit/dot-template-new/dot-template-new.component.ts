import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs/operators';

import { DialogService } from 'primeng/dynamicdialog';

import { DotTemplateSelectorComponent } from '@portlets/dot-templates/dot-template-list/components/dot-template-selector/dot-template-selector.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';

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
