import { Observable, Subject } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';

import { DialogModule } from 'primeng/dialog';

import { map, takeUntil, tap } from 'rxjs/operators';

import { DotEventsService } from '@dotcms/data-access';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentCompareComponent } from '../../dot-content-compare.component';

const COMPARE_CUSTOM_EVENT = 'compare-contentlet';

@Component({
    selector: 'dot-content-compare-dialog',
    templateUrl: './dot-content-compare-dialog.component.html',
    styleUrls: ['./dot-content-compare-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DialogModule, DotContentCompareComponent, DotMessagePipe, AsyncPipe]
})
export class DotContentCompareDialogComponent implements OnInit, OnDestroy {
    private dotEventsService = inject(DotEventsService);
    private cdr = inject(ChangeDetectorRef);

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
                this.cdr.detectChanges();
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

    /**
     * Sync dialog visibility from PrimeNG; only tear down when closing.
     * @param {boolean} visible
     */
    onVisibleChange(visible: boolean): void {
        if (!visible) {
            this.close();
        }
    }
}
