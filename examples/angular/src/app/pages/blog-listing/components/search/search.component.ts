import { Component, effect, input, model, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  imports: [ReactiveFormsModule],
})
export class SearchComponent {
  query = input<string>('');
  searchQueryChange = output<string>();

  inputControl = new FormControl<string>('', { nonNullable: true });

  constructor() {
    this.handleSearchQueryChange();
    effect(() => {
      const query = this.query();
      if (query) {
        this.inputControl.setValue(query);
      }
    });
  }

  handleSearchQueryChange(): void {
    this.inputControl.valueChanges
      .pipe(debounceTime(500), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => {
        this.searchQueryChange.emit(value);
      });
  }
}
