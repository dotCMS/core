import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';

import { DotCMSContentType, DotPagination } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotUvePaletteItemComponent } from '../dot-uve-palette-item/dot-uve-palette-item.component';

@Component({
    selector: 'dot-uve-palette-list',
    imports: [
        DotUvePaletteItemComponent,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        PaginatorModule,
        DotMessagePipe
    ],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent {
    $contentTypes = input.required<DotCMSContentType[]>({ alias: 'contentTypes' });
    $pagination = input.required<DotPagination>({ alias: 'pagination' });

    // Computed signal for paginated content types
    readonly $start = computed(() => {
        return (this.$pagination().currentPage - 1) * this.$pagination().perPage;
    });
    readonly $rowsPerPage = computed(() => {
        return this.$pagination().perPage;
    });
    readonly $totalRecords = computed(() => {
        return this.$pagination().totalEntries;
    });

    onPageChange(_event: PaginatorState) {
        // Do nothing for now
    }
}
