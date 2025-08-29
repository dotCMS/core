import { Component, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'app-search',
    templateUrl: './search.component.html',
    imports: [ReactiveFormsModule]
})
export class SearchComponent {
    searchQueryChange = output<string>();

    inputControl = new FormControl<string>('', { nonNullable: true });

    constructor() {
      this.handleSearchQueryChange();
    }

    handleSearchQueryChange(): void {
      this.inputControl.valueChanges
        .pipe(
          debounceTime(500),
          distinctUntilChanged(),
          filter((value) => value.length > 3),
          takeUntilDestroyed(),
        )
        .subscribe((value) => {
          this.searchQueryChange.emit(value);
        });
    }
}
