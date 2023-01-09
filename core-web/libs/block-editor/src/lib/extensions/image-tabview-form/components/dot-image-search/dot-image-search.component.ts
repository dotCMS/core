import {
    Component,
    OnInit,
    OnDestroy,
    ViewChild,
    Input,
    ElementRef,
    Output,
    EventEmitter
} from '@angular/core';
import { BehaviorSubject, Subject, fromEvent } from 'rxjs';
import { debounceTime, throttleTime, skip, takeUntil } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

// services
import { DotImageSearchStore } from './store/dot-image-search.store';
import { AfterViewInit, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'dot-image-search',
    templateUrl: './dot-image-search.component.html',
    styleUrls: ['./dot-image-search.component.scss'],
    providers: [DotImageSearchStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageSearchComponent implements OnInit, OnDestroy, AfterViewInit {
    @ViewChild('input') input!: ElementRef;
    @Output() addImage = new EventEmitter<DotCMSContentlet>();

    @Input() set languageId(id) {
        this.store.updatelanguageId(id);
    }

    vm$ = this.store.vm$;
    offset$ = new BehaviorSubject<number>(0);

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private store: DotImageSearchStore) {}

    ngOnInit(): void {
        this.offset$
            .pipe(takeUntil(this.destroy$), skip(1), throttleTime(450))
            .subscribe(this.store.nextBatch);

        requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    ngAfterViewInit() {
        fromEvent(this.input.nativeElement, 'input')
            .pipe(takeUntil(this.destroy$), debounceTime(450))
            .subscribe(({ target }) => {
                this.store.searchContentlet(target.value);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
    }
}
