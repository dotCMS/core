import { Component, OnInit, Input } from '@angular/core';
import { MenuItem } from 'primeng/primeng';
import { DotMessageService } from '@services/dot-messages-service';

@Component({
    selector: 'dot-template-addtional-actions-menu',
    templateUrl: './dot-template-additional-actions-menu.component.html'
})
export class DotTemplateAdditionalActionsMenuComponent implements OnInit {
    @Input() inode: string;
    items: MenuItem[];

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.items = [
            {
                label: this.dotMessageService.get('template.action.additional.properties'),
                routerLink: `template/${this.inode}/properties`
            },
            {
                label: this.dotMessageService.get('template.action.additional.permissions'),
                routerLink: `template/${this.inode}/permissions`
            },
            {
                label: this.dotMessageService.get('template.action.additional.history'),
                routerLink: `template/${this.inode}/history`
            }
        ];
    }
}
