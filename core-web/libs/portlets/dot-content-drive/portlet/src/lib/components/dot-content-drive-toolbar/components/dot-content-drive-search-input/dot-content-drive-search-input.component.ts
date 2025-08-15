import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-search-input',
    templateUrl: './dot-content-drive-search-input.component.html',
    styleUrl: './dot-content-drive-search-input.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [IconFieldModule, InputIconModule, InputTextModule, ReactiveFormsModule]
})
export class DotContentDriveSearchInputComponent implements OnInit, OnDestroy {
    readonly #store = inject(DotContentDriveStore);
    readonly #destroy$ = new Subject<void>();

    readonly searchControl = new FormControl('');

    ngOnInit(): void {
        const searchValue = this.#store.getFilterValue('title');

        if (searchValue) {
            this.searchControl.setValue(searchValue as string);
        }

        this.searchControl.valueChanges
            .pipe(debounceTime(500), distinctUntilChanged(), takeUntil(this.#destroy$))
            .subscribe((value) => {
                const searchValue = (value as string)?.trim() || '';
                if (searchValue) {
                    this.#store.setFilters({ title: searchValue });
                } else {
                    this.#store.removeFilter('title');
                }
            });
    }

    ngOnDestroy(): void {
        this.#destroy$.next();
        this.#destroy$.complete();
    }
}
