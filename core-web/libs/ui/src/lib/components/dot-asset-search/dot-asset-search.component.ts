import { BehaviorSubject, fromEvent } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';

import { debounceTime, skip, throttleTime } from 'rxjs/operators';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

// services

import { DotAssetCardListComponent } from './components/dot-asset-card-list/dot-asset-card-list.component';
import { DotAssetSearchStore } from './store/dot-asset-search.store';

@Component({
    selector: 'dot-asset-search',
    templateUrl: './dot-asset-search.component.html',
    styleUrls: ['./dot-asset-search.component.scss'],
    providers: [DotAssetSearchStore],
    imports: [DotAssetCardListComponent, InputText, IconField, InputIcon, CommonModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetSearchComponent implements OnInit, AfterViewInit {
    @ViewChild('input') input!: ElementRef;
    @Output() addAsset = new EventEmitter<DotCMSContentlet>();

    @Input() languageId = '*';
    @Input() type: EditorAssetTypes;

    private currentSearch = '';
    private readonly store = inject(DotAssetSearchStore);
    private readonly destroyRef = inject(DestroyRef);

    offset$ = new BehaviorSubject<number>(0);
    vm$ = this.store.vm$;

    ngOnInit(): void {
        // Initial load
        this.store.searchContentlet({
            ...this.searchParams(),
            search: '',
            offset: 0
        });

        this.offset$
            .pipe(takeUntilDestroyed(this.destroyRef), skip(1), throttleTime(450))
            .subscribe((offset) =>
                this.store.nextBatch({
                    ...this.searchParams(),
                    offset
                })
            );

        requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    ngAfterViewInit() {
        fromEvent(this.input.nativeElement, 'input')
            .pipe(takeUntilDestroyed(this.destroyRef), debounceTime(450))
            .subscribe((event: Event) => {
                const target = event.target as HTMLInputElement;
                const value = (target as HTMLInputElement).value;
                this.currentSearch = value;
                this.store.searchContentlet({
                    ...this.searchParams(),
                    search: value
                });
            });
    }

    private searchParams() {
        return {
            languageId: this.languageId || '',
            search: this.currentSearch,
            assetType: this.type,
            offset: this.offset$.value || 0
        };
    }
}
