import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';

import { map, takeUntil, tap } from 'rxjs/operators';

import { DotEventsService } from '@dotcms/data-access';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';
import { DotDialogComponent, DotMessagePipe } from '@dotcms/ui';

import { DotContentCompareComponent } from '../../dot-content-compare.component';

const COMPARE_CUSTOM_EVENT = 'compare-contentlet';

@Component({
    selector: 'dot-content-compare-dialog',
    templateUrl: './dot-content-compare-dialog.component.html',
    styleUrls: ['./dot-content-compare-dialog.component.scss'],
    imports: [CommonModule, DotDialogComponent, DotContentCompareComponent, DotMessagePipe]
})
export class DotContentCompareDialogComponent implements OnInit, OnDestroy {
    private dotEventsService = inject(DotEventsService);

    show = false;
    data$: Observable<DotContentCompareEvent>;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.data$ = this.dotEventsService.listen(COMPARE_CUSTOM_EVENT).pipe(
            takeUntil(this.destroy$),
            map((x) => x?.data),
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
