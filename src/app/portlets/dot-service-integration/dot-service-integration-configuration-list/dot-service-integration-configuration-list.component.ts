import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import {
    DotServiceIntegration,
    DotServiceIntegrationSites
} from '@shared/models/dot-service-integration/dot-service-integration.model';
import { ActivatedRoute } from '@angular/router';
import { pluck, takeUntil, take } from 'rxjs/operators';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { Subject } from 'rxjs';
import { DotRouterService } from '@services/dot-router/dot-router.service';

@Component({
    selector: 'dot-service-integration-configuration-list',
    templateUrl: './dot-service-integration-configuration-list.component.html',
    styleUrls: ['./dot-service-integration-configuration-list.component.scss']
})
export class DotServiceIntegrationConfigurationListComponent implements OnInit, OnDestroy {
    @ViewChild('searchInput')
    searchInput: ElementRef;
    messagesKey: { [key: string]: string } = {};
    serviceIntegration: DotServiceIntegration;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotServiceIntegrationService: DotServiceIntegrationService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute
    ) {}

    ngOnInit() {
        this.route.data
            .pipe(pluck('data'), takeUntil(this.destroy$))
            .subscribe(([integration, messages]) => {
                this.serviceIntegration = integration;
                this.messagesKey = messages;
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Redirects to create configuration page
     *
     * @memberof DotServiceIntegrationConfigurationListComponent
     */
    createConfiguration(): void {
        this.dotRouterService.gotoPortlet(
            `/integration-services/${this.serviceIntegration.key}/new`
        );
    }

    /**
     * Redirects to edit configuration page
     *
     * @param MouseEvent $event
     * @param string configurationId
     * @memberof DotServiceIntegrationConfigurationListComponent
     */
    editConfiguration($event: MouseEvent, configurationId: string): void {
        $event.stopPropagation();
        this.dotRouterService.gotoPortlet(
            `/integration-services/${this.serviceIntegration.key}/edit/${configurationId}`
        );
    }

    /**
     * Display confirmation dialog to delete a specific configuration
     *
     * @param MouseEvent $event
     * @param DotServiceIntegrationSites site
     * @memberof DotServiceIntegrationConfigurationListComponent
     */
    deleteConfiguration($event: MouseEvent, site: DotServiceIntegrationSites): void {
        $event.stopPropagation();
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotServiceIntegrationService
                    .deleteConfiguration(this.serviceIntegration.key, site.id)
                    .pipe(take(1))
                    .subscribe(() => this.getConfiguration());
            },
            reject: () => {},
            header: this.messagesKey['service.integration.confirmation.title'],
            message: `${this.messagesKey['service.integration.confirmation.delete.message']} <b>${site.name}</b> ?`,
            footerLabel: {
                accept: this.messagesKey['service.integration.confirmation.accept']
            }
        });
    }

    /**
     * Display confirmation dialog to delete all configurations
     *
     * @memberof DotServiceIntegrationConfigurationListComponent
     */
    deleteAllConfigurations(): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotServiceIntegrationService
                    .deleteAllConfigurations(this.serviceIntegration.key)
                    .pipe(take(1))
                    .subscribe(() => this.getConfiguration());
            },
            reject: () => {},
            header: this.messagesKey['service.integration.confirmation.title'],
            message: this.messagesKey['service.integration.confirmation.delete.all.message'],
            footerLabel: {
                accept: this.messagesKey['service.integration.confirmation.accept']
            }
        });
    }

    private getConfiguration() {
        this.dotServiceIntegrationService
            .getConfiguration(this.serviceIntegration.key)
            .pipe(take(1))
            .subscribe(
                (configuration: DotServiceIntegration) => (this.serviceIntegration = configuration)
            );
    }
}
