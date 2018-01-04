import { Component, OnInit, Input } from '@angular/core';
import { MenuItem } from 'primeng/primeng';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';


@Component({
    selector: 'dot-template-addtional-actions-menu',
    templateUrl: './dot-template-additional-actions-menu.component.html'
})
export class DotTemplateAdditionalActionsMenuComponent implements OnInit {
    @Input() templateId: string;
    items: MenuItem[];

    constructor(private messageService: DotMessageService) {

    }

    ngOnInit() {
        const keys = [
            'template.action.additional.permissions',
            'template.action.additional.history',
            'template.action.additional.properties'
        ];

        this.messageService.getMessages(keys).subscribe(messages => {
            this.items = [
                {
                    label: messages['template.action.additional.properties'],
                    routerLink: `template/${this.templateId}/properties`
                },
                {
                    label: messages['template.action.additional.permissions'],
                    routerLink: `template/${this.templateId}/permissions`
                },
                {
                    label: messages['template.action.additional.history'],
                    routerLink: `template/${this.templateId}/history`
                }
            ];
        });
    }
}
