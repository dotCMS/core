import { Component, signal, output, input } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-search',
    templateUrl: './search.component.html',
    standalone: true,
    imports: [FormsModule]
})
export class SearchComponent {
    searchQuery = input<string>('');
    searchQueryChange = output<string>();

    onSearchChange(event: Event): void {
        const value = (event.target as HTMLInputElement).value;
        this.searchQueryChange.emit(value);
    }
}
