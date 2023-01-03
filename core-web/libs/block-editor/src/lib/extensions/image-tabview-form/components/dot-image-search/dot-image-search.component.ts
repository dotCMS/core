import {
    Component,
    OnInit,
    OnDestroy,
    ViewChild,
    Input,
    ElementRef,
    ChangeDetectionStrategy,
    Output,
    EventEmitter
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { BehaviorSubject, Subject } from 'rxjs';
import { debounceTime, throttleTime, skip, takeUntil } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

// services
import { DotImageSearchStore } from './dot-image-search.store';

@Component({
    selector: 'dot-image-search',
    templateUrl: './dot-image-search.component.html',
    styleUrls: ['./dot-image-search.component.scss'],
    providers: [DotImageSearchStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageSearchComponent implements OnInit, OnDestroy {
    @ViewChild('inputSearch') inputSearch!: ElementRef;

    @Output() seletedImage = new EventEmitter<DotCMSContentlet>();
    @Input() set languageId(id) {
        this.store.updateLanguages(id);
    }

    vm$ = this.store.vm$;
    offset$ = new BehaviorSubject<number>(0);
    private destroy$: Subject<boolean> = new Subject<boolean>();

    form: FormGroup;

    constructor(private store: DotImageSearchStore, private fb: FormBuilder) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            search: ['']
        });

        this.form.valueChanges
            .pipe(takeUntil(this.destroy$), debounceTime(450))
            .subscribe(({ search }) => {
                this.store.searchContentlet(search);
            });

        this.offset$
            .pipe(takeUntil(this.destroy$), skip(1), throttleTime(450))
            .subscribe((offset) => {
                this.store.nextBatch(offset * 2);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
    }
}
