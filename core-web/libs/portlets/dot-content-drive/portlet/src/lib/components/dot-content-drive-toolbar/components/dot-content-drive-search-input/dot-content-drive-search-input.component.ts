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
    #store = inject(DotContentDriveStore);
    #destroy$ = new Subject<void>();

    searchControl = new FormControl('');

    ngOnInit(): void {
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
