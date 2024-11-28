import { Component, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

@Component({
    selector: 'dot-pagination',
    standalone: true,
    imports: [ButtonModule],
    templateUrl: './pagination.component.html'
})
export class PaginationComponent {
    /**
     * A signal that holds the total number of pages.
     * It is used to display the total number of pages in the pagination component.
     */
    $totalPages = input.required<number>({ alias: 'totalPages' });

    /**
     * A signal that holds the current page number.
     * It is used to display the current page number in the pagination component.
     */
    $currentPage = input.required<number>({ alias: 'currentPage' });

    /**
     * An output signal that emits when the previous page button is clicked.
     */
    previousPage = output<void>();

    /**
     * An output signal that emits when the next page button is clicked.
     */
    nextPage = output<void>();
}
