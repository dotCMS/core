import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';
import { DotAppsSites } from '@shared/models/dot-apps/dot-apps.model';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';

import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-apps-configuration-item',
    templateUrl: './dot-apps-configuration-item.component.html',
    styleUrls: ['./dot-apps-configuration-item.component.scss']
})
export class DotAppsConfigurationItemComponent {
    @Input() site: DotAppsSites;

    @Output() edit = new EventEmitter<DotAppsSites>();
    @Output() delete = new EventEmitter<DotAppsSites>();

    constructor(
        private dotMessageService: DotMessageService,
        private dotAlertConfirmService: DotAlertConfirmService
    ) {}

    @HostListener('click', ['$event'])
    public onClick(event: MouseEvent): void {
        event.stopPropagation();
        this.edit.emit(this.site);
    }

    /**
     * Emits action to edit configuration page
     *
     * @param MouseEvent $event
     * @param DotAppsSites site
     * @memberof DotAppsConfigurationItemComponent
     */
    editConfigurationSite($event: MouseEvent, site?: DotAppsSites): void {
        $event.stopPropagation();
        this.edit.emit(site);
    }

    /**
     * Display confirmation dialog to delete a specific configuration
     *
     * @param MouseEvent $event
     * @param DotAppsSites site
     * @memberof DotAppsConfigurationItemComponent
     */
    confirmDelete($event: MouseEvent, site: DotAppsSites): void {
        $event.stopPropagation();
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.delete.emit(site);
            },
            reject: () => {},
            header: this.dotMessageService.get('apps.confirmation.title'),
            message: `${this.dotMessageService.get(
                'apps.confirmation.delete.message'
            )} <b>${site.name}</b> ?`,
            footerLabel: {
                accept: this.dotMessageService.get('apps.confirmation.accept')
            }
        });
    }
}
