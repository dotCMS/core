import { BehaviorSubject, fromEvent, Subject } from 'rxjs';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { debounceTime, skip, takeUntil, throttleTime } from 'rxjs/operators';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

// services
import { DotAssetSearchStore } from './store/dot-asset-search.store';

@Component({
    selector: 'dot-asset-search',
    templateUrl: './dot-asset-search.component.html',
    styleUrls: ['./dot-asset-search.component.scss'],
    providers: [DotAssetSearchStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetSearchComponent implements OnInit, OnDestroy, AfterViewInit {
    @ViewChild('input') input!: ElementRef;
    @Output() addAsset = new EventEmitter<DotCMSContentlet>();

    @Input() set languageId(id) {
        this.store.updatelanguageId(id);
    }

    @Input() set type(type: EditorAssetTypes) {
        this.store.updateAssetType(type);
    }

    vm$ = this.store.vm$;
    offset$ = new BehaviorSubject<number>(0);

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private store: DotAssetSearchStore) {}

    ngOnInit(): void {
        this.store.searchContentlet('');

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
