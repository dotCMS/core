import { Observable, Subject } from 'rxjs';

import { Component, OnDestroy, OnInit, inject } from '@angular/core';

import { map, pluck, takeUntil, tap } from 'rxjs/operators';

import { DotEventsService } from '@dotcms/data-access';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';

const COMPARE_CUSTOM_EVENT = 'compare-contentlet';

@Component({
    selector: 'dot-content-compare-dialog',
    templateUrl: './dot-content-compare-dialog.component.html',
    styleUrls: ['./dot-content-compare-dialog.component.scss'],
    standalone: false
})
export class DotContentCompareDialogComponent implements OnInit, OnDestroy {
    private dotEventsService = inject(DotEventsService);

    show = false;
    data$: Observable<DotContentCompareEvent>;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.data$ = this.dotEventsService.listen(COMPARE_CUSTOM_EVENT).pipe(
            takeUntil(this.destroy$),
            pluck('data'),
            map((data: DotContentCompareEvent) => data),
            tap(() => {
                this.show = true;
            })
        );
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    close(): void {
        this.show = false;
    }
}
