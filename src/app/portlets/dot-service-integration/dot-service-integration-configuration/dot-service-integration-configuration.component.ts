import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import {
    DotServiceIntegration,
    DotServiceIntegrationSites
} from '@shared/models/dot-service-integration/dot-service-integration.model';
import { ActivatedRoute } from '@angular/router';
import { pluck, take, debounceTime } from 'rxjs/operators';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { fromEvent as observableFromEvent } from 'rxjs';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { LazyLoadEvent } from 'primeng/primeng';
import { PaginatorService } from '@services/paginator';
import { IntegrationResolverData } from './dot-service-integration-configuration-resolver.service';

@Component({
    selector: 'dot-service-integration-configuration',
    templateUrl: './dot-service-integration-configuration.component.html',
    styleUrls: ['./dot-service-integration-configuration.component.scss']
})
export class DotServiceIntegrationConfigurationComponent implements OnInit {
    @ViewChild('searchInput')
    searchInput: ElementRef;
    messagesKey: { [key: string]: string } = {};
    serviceIntegration: DotServiceIntegration;

    disabledLoadDataButton: boolean;
    paginationPerPage = 10;
    totalRecords: number;
    showMore: boolean;

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotServiceIntegrationService: DotServiceIntegrationService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute,
        public paginationService: PaginatorService
    ) {}

    ngOnInit() {
        this.route.data
            .pipe(pluck('data'), take(1))
            .subscribe(({ messages, service }: IntegrationResolverData) => {
                this.serviceIntegration = service;
                this.serviceIntegration.sites = [];
                this.messagesKey = messages;
            });

        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500))
            .subscribe((keyboardEvent: Event) => {
                this.filterConfigurations(keyboardEvent.target['value']);
            });

        this.paginationService.url = `v1/service-integrations/${this.serviceIntegration.key}`;
        this.paginationService.paginationPerPage = this.paginationPerPage;
        this.paginationService.sortField = 'name';
        this.paginationService.setExtraParams('filter', '');
        this.paginationService.sortOrder = 1;
        this.loadData();

        this.searchInput.nativeElement.focus();
    }

    /**
     * Loads data through pagination service
     *
     * @param LazyLoadEvent event
     * @memberof DotServiceIntegrationConfigurationComponent
     */
    loadData(event?: LazyLoadEvent) {
        this.paginationService
            .getWithOffset((event && event.first) || 0)
            .pipe(take(1), pluck('sites'))
            .subscribe((sites: DotServiceIntegrationSites[]) => {
                this.serviceIntegration.sites = event
                    ? this.serviceIntegration.sites.concat(sites)
                    : sites;
                this.totalRecords = this.paginationService.totalRecords;
                this.disabledLoadDataButton = !this.isThereMoreData(
                    this.serviceIntegration.sites.length
                );
            });
    }

    /**
     * Redirects to create/edit configuration site page
     *
     * @param DotServiceIntegrationSites site
     * @memberof DotServiceIntegrationConfigurationComponent
     */
    gotoConfiguration(site: DotServiceIntegrationSites): void {
        this.dotRouterService.goToIntegrationService(this.serviceIntegration.key, site);
    }

    /**
     * Display confirmation dialog to delete a specific configuration
     *
     * @param DotServiceIntegrationSites site
     * @memberof DotServiceIntegrationConfigurationComponent
     */
    deleteConfiguration(site: DotServiceIntegrationSites): void {
        this.dotServiceIntegrationService
            .deleteConfiguration(this.serviceIntegration.key, site.id)
            .pipe(take(1))
            .subscribe(() => {
                this.serviceIntegration.sites = [];
                this.loadData();
            });
    }

    /**
     * Display confirmation dialog to delete all configurations
     *
     * @memberof DotServiceIntegrationConfigurationComponent
     */
    deleteAllConfigurations(): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotServiceIntegrationService
                    .deleteAllConfigurations(this.serviceIntegration.key)
                    .pipe(take(1))
                    .subscribe(() => {
                        this.serviceIntegration.sites = [];
                        this.loadData();
                    });
            },
            reject: () => {},
            header: this.messagesKey['service.integration.confirmation.title'],
            message: this.messagesKey['service.integration.confirmation.delete.all.message'],
            footerLabel: {
                accept: this.messagesKey['service.integration.confirmation.accept']
            }
        });
    }

    private isThereMoreData(index: number): boolean {
        return this.totalRecords / index > 1;
    }

    private filterConfigurations(searchCriteria?: string): void {
        this.paginationService.setExtraParams('filter', searchCriteria);
        this.loadData();
    }
}
