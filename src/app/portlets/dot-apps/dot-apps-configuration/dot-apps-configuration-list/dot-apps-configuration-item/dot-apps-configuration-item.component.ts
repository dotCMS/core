import { Component, OnInit, Input, Output, EventEmitter, HostListener } from '@angular/core';
import { DotAppsSites } from '@shared/models/dot-apps/dot-apps.model';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';

import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-apps-configuration-item',
    templateUrl: './dot-apps-configuration-item.component.html',
    styleUrls: ['./dot-apps-configuration-item.component.scss']
})
export class DotAppsConfigurationItemComponent implements OnInit {
    @Input() site: DotAppsSites;

    @Output() edit = new EventEmitter<DotAppsSites>();
    @Output() delete = new EventEmitter<DotAppsSites>();

    messagesKey: { [key: string]: string } = {};

    constructor(
        public dotMessageService: DotMessageService,
        private dotAlertConfirmService: DotAlertConfirmService
    ) {}

    @HostListener('click', ['$event'])
    public onClick(event: MouseEvent): void {
        event.stopPropagation();
        this.edit.emit(this.site);
    }

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'apps.key',
                'apps.confirmation.title',
                'apps.confirmation.delete.message',
                'apps.confirmation.accept',
                'apps.invalid.secrets'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
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
            header: this.messagesKey['apps.confirmation.title'],
            message: `${this.messagesKey['apps.confirmation.delete.message']} <b>${site.name}</b> ?`,
            footerLabel: {
                accept: this.messagesKey['apps.confirmation.accept']
            }
        });
    }
}
