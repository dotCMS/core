import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-search-input',
    templateUrl: './dot-content-drive-search-input.component.html',
    styleUrl: './dot-content-drive-search-input.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [IconFieldModule, InputIconModule, InputTextModule, ReactiveFormsModule]
})
export class DotContentDriveSearchInputComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly searchControl = new FormControl('');

    ngOnInit() {
        const searchValue = this.#store.getFilterValue('title');

        if (searchValue) {
            this.searchControl.setValue(searchValue as string);
        }

        this.searchControl.valueChanges
            .pipe(takeUntilDestroyed(this.#destroyRef), debounceTime(500), distinctUntilChanged())
            .subscribe((value) => {
                const searchValue = (value as string)?.trim() || '';
                if (searchValue) {
                    this.#store.patchFilters({ title: searchValue });
                } else {
                    this.#store.removeFilter('title');
                }
            });
    }
}
