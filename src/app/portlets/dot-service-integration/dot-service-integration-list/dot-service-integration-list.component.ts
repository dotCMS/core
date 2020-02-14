import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { take, debounceTime, pluck, takeUntil } from 'rxjs/operators';
import { fromEvent as observableFromEvent, Subject } from 'rxjs';
import { DotServiceIntegration } from '@shared/models/dot-service-integration/dot-service-integration.model';
import * as _ from 'lodash';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'dot-service-integration-list',
    templateUrl: './dot-service-integration-list.component.html',
    styleUrls: ['./dot-service-integration-list.component.scss']
})
export class DotServiceIntegrationListComponent implements OnInit, OnDestroy {
    @ViewChild('searchInput')
    searchInput: ElementRef;
    messagesKey: { [key: string]: string } = {};
    serviceIntegrations: DotServiceIntegration[];
    serviceIntegrationsCopy: DotServiceIntegration[];

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public dotMessageService: DotMessageService,
        private route: ActivatedRoute,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['service.integration.search.placeholder'])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });

        this.route.data
            .pipe(pluck('integrationServices'), takeUntil(this.destroy$))
            .subscribe((integrations: DotServiceIntegration[]) => {
                this.serviceIntegrations = integrations;
                this.serviceIntegrationsCopy = _.cloneDeep(integrations);
            });

        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500))
            .subscribe((keyboardEvent: Event) => {
                this.filterIntegrations(keyboardEvent.target['value']);
            });

        this.searchInput.nativeElement.focus();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Redirects to service configuration listing page
     *
     * @param string key
     * @memberof DotServiceIntegrationListComponent
     */
    goToIntegration(key: string): void {
        this.dotRouterService.gotoPortlet(`/integration-services/${key}`);
    }

    private filterIntegrations(searchCriteria?: string): void {
        this.serviceIntegrationsCopy = this.serviceIntegrations.filter(
            (integration: DotServiceIntegration) =>
                integration.name.toUpperCase().search(searchCriteria.toUpperCase()) >= 0
        );
    }
}
