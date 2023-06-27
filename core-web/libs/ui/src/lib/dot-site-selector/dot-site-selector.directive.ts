import { Subject } from 'rxjs';

import {
    ChangeDetectorRef,
    Directive,
    Input,
    OnDestroy,
    OnInit,
    Optional,
    Self
} from '@angular/core';

import { Dropdown } from 'primeng/dropdown';

import { debounceTime, take, takeUntil } from 'rxjs/operators';

import { DotEventsService, PaginatorService, DotSiteService } from '@dotcms/data-access';
import { Site } from '@dotcms/dotcms-js';

@Directive({
    selector: '[dotSiteSelector]',
    providers: [PaginatorService],
    standalone: true
})
export class DotSiteSelectorDirective implements OnInit, OnDestroy {
    @Input() archive = false;
    @Input() live = true;
    @Input() system = true;
    @Input() pageSize = 10;

    private readonly destroy$: Subject<boolean> = new Subject<boolean>();
    private readonly dotEvents = ['login-as', 'logout-as'];
    private readonly control: Dropdown;

    constructor(
        @Optional() @Self() private readonly primeDropdown: Dropdown,
        private readonly dotEventsService: DotEventsService,
        private readonly dotSiteService: DotSiteService,
        private readonly cd: ChangeDetectorRef
    ) {
        this.control = this.primeDropdown;

        if (this.control) {
            this.control.onFilter.pipe(debounceTime(300)).subscribe((event) => {
                this.getSitesList(event.filter);
            });
        } else {
            console.warn('ContainerOptionsDirective is for use with PrimeNg Dropdown');
        }
    }

    ngOnInit() {
        this.getSitesList();

        this.dotEvents.forEach((event: string) => {
            this.dotEventsService
                .listen(event)
                .pipe(takeUntil(this.destroy$))
                .subscribe(() => this.getSitesList());
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Set the options of the dropdown
     * @param options
     */
    private setOptions(options: Array<Site>): void {
        this.primeDropdown.options = [...options];
        this.cd.detectChanges();
    }

    /**
     * Call to load a new page.
     * @param string [filter='']
     * @memberof SiteSelectorComponent
     */
    private getSitesList(filter = ''): void {
        this.dotSiteService
            .getSites(filter, this.pageSize)
            .pipe(take(1))
            .subscribe((items: Site[]) => this.setOptions(items));
    }
}
