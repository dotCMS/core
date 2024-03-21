import { BehaviorSubject, fromEvent, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
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

import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, skip, takeUntil, throttleTime } from 'rxjs/operators';

import { DotContentSearchService, DotLanguagesService } from '@dotcms/data-access';
import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

// services
import { DotAssetCardComponent } from './components/dot-asset-card/dot-asset-card.component';
import { DotAssetCardListComponent } from './components/dot-asset-card-list/dot-asset-card-list.component';
import { DotAssetCardSkeletonComponent } from './components/dot-asset-card-skeleton/dot-asset-card-skeleton.component';
import { DotAssetSearchStore } from './store/dot-asset-search.store';

@Component({
    selector: 'dot-asset-search',
    templateUrl: './dot-asset-search.component.html',
    styleUrls: ['./dot-asset-search.component.scss'],
    providers: [DotAssetSearchStore, DotContentSearchService, DotLanguagesService],
    standalone: true,
    imports: [
        DotAssetCardComponent,
        DotAssetCardListComponent,
        DotAssetCardSkeletonComponent,
        DotAssetCardListComponent,
        InputTextModule,
        CommonModule
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetSearchComponent implements OnInit, OnDestroy, AfterViewInit {
    @ViewChild('input') input!: ElementRef;
    @Output() addAsset = new EventEmitter<DotCMSContentlet>();

    @Input() set languageId(id) {
        this.store.updatelanguageId(id);
    }

    private _assetType: EditorAssetTypes;
    @Input() set type(type: EditorAssetTypes) {
        this._assetType = type;
        this.store.updateAssetType(type);
    }

    vm$ = this.store.vm$;
    offset$ = new BehaviorSubject<number>(0);

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private store: DotAssetSearchStore) {}

    ngOnInit(): void {
        this.store.init(this._assetType);

        this.offset$
            .pipe(takeUntil(this.destroy$), skip(1), throttleTime(450))
            .subscribe((offset) => this.store.nextBatch(offset));

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
