import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotAppsSite } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-apps-configuration-item',
    templateUrl: './dot-apps-configuration-item.component.html',
    styleUrls: ['./dot-apps-configuration-item.component.scss']
})
export class DotAppsConfigurationItemComponent {
    @Input() site: DotAppsSite;

    @Output() edit = new EventEmitter<DotAppsSite>();
    @Output() export = new EventEmitter<DotAppsSite>();
    @Output() delete = new EventEmitter<DotAppsSite>();

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
    editConfigurationSite($event: MouseEvent, site?: DotAppsSite): void {
        $event.stopPropagation();
        this.edit.emit(site);
    }

    /**
     * Emits action to export configuration
     *
     * @param MouseEvent $event
     * @param DotAppsSites site
     * @memberof DotAppsConfigurationItemComponent
     */
    exportConfiguration($event: MouseEvent, site: DotAppsSite): void {
        $event.stopPropagation();
        this.export.emit(site);
    }

    /**
     * Display confirmation dialog to delete a specific configuration
     *
     * @param MouseEvent $event
     * @param DotAppsSites site
     * @memberof DotAppsConfigurationItemComponent
     */
    confirmDelete($event: MouseEvent, site: DotAppsSite): void {
        $event.stopPropagation();
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.delete.emit(site);
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('apps.confirmation.title'),
            message: `${this.dotMessageService.get('apps.confirmation.delete.message')} <b>${
                site.name
            }</b> ?`,
            footerLabel: {
                accept: this.dotMessageService.get('apps.confirmation.accept')
            }
        });
    }
}
