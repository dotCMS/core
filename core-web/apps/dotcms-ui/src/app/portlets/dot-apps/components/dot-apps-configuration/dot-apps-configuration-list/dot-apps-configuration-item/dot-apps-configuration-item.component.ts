import { Component, HostListener, inject, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotAppsSite } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCopyLinkComponent } from '../../../../../../view/components/dot-copy-link/dot-copy-link.component';

@Component({
    selector: 'dot-apps-configuration-item',
    templateUrl: './dot-apps-configuration-item.component.html',
    styleUrls: ['./dot-apps-configuration-item.component.scss'],
    imports: [DotCopyLinkComponent, TooltipModule, DotMessagePipe, ButtonModule]
})
export class DotAppsConfigurationItemComponent {
    private dotMessageService = inject(DotMessageService);
    private dotAlertConfirmService = inject(DotAlertConfirmService);

    site = input<DotAppsSite>();

    edit = output<DotAppsSite>();
    export = output<DotAppsSite>();
    delete = output<DotAppsSite>();

    @HostListener('click', ['$event'])
    public onClick(event: MouseEvent): void {
        event.stopPropagation();
        this.edit.emit(this.site());
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
     * @memberof DotAppsConfigurationItemComponent
     */
    confirmDelete($event: MouseEvent): void {
        $event.stopPropagation();
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.delete.emit(this.site());
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('apps.confirmation.title'),
            message: `${this.dotMessageService.get('apps.confirmation.delete.message')} <b>${
                this.site().name
            }</b> ?`,
            footerLabel: {
                accept: this.dotMessageService.get('apps.confirmation.accept')
            }
        });
    }
}
