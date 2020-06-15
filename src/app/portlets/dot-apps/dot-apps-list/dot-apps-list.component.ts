import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { debounceTime, pluck, takeUntil } from 'rxjs/operators';
import { fromEvent as observableFromEvent, Subject } from 'rxjs';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';
import * as _ from 'lodash';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActivatedRoute } from '@angular/router';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';

@Component({
    selector: 'dot-apps-list',
    templateUrl: './dot-apps-list.component.html',
    styleUrls: ['./dot-apps-list.component.scss']
})
export class DotAppsListComponent implements OnInit, OnDestroy {
    @ViewChild('searchInput')
    searchInput: ElementRef;
    apps: DotApps[];
    appsCopy: DotApps[];

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private route: ActivatedRoute,
        private dotRouterService: DotRouterService,
        private dotAppsService: DotAppsService
    ) {}

    ngOnInit() {
        this.route.data
            .pipe(pluck('appsServices'), takeUntil(this.destroy$))
            .subscribe((apps: DotApps[]) => {
                this.apps = apps;
                this.appsCopy = _.cloneDeep(apps);
            });

        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntil(this.destroy$))
            .subscribe((keyboardEvent: Event) => {
                this.filterApps(keyboardEvent.target['value']);
            });

        this.searchInput.nativeElement.focus();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Redirects to apps configuration listing page
     *
     * @param string key
     * @memberof DotAppsListComponent
     */
    goToApp(key: string): void {
        this.dotRouterService.goToAppsConfiguration(key);
    }

    private filterApps(searchCriteria?: string): void {
        this.dotAppsService.get(searchCriteria).subscribe((apps: DotApps[]) => {
            this.appsCopy = apps;
        });
    }
}
