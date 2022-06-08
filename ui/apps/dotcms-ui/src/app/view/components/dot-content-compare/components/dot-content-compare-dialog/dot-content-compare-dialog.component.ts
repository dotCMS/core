import { Component, OnDestroy, OnInit } from '@angular/core';
import { map, pluck, takeUntil, tap } from 'rxjs/operators';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { COMPARE_CUSTOM_EVENT } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { Observable, Subject } from 'rxjs';
import { DotContentCompareEvent } from '@components/dot-content-compare/dot-content-compare.component';

@Component({
    selector: 'dot-content-compare-dialog',
    templateUrl: './dot-content-compare-dialog.component.html',
    styleUrls: ['./dot-content-compare-dialog.component.scss']
})
export class DotContentCompareDialogComponent implements OnInit, OnDestroy {
    show = false;
    data$: Observable<DotContentCompareEvent>;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private dotEventsService: DotEventsService) {}

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
