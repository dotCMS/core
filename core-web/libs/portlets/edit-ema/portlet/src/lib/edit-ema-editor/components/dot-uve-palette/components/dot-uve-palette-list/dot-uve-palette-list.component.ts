import { patchState, signalState } from '@ngrx/signals';

import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';

import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { MOCK_CONTENT_TYPES } from '../../utils';
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
    $contentTypes = signal<DotCMSContentType[]>([
        ...MOCK_CONTENT_TYPES,
        ...MOCK_CONTENT_TYPES,
        ...MOCK_CONTENT_TYPES,
        ...MOCK_CONTENT_TYPES
    ]);

    readonly state = signalState({
        $first: 0,
        $rows: 20,
        $totalRecords: this.$contentTypes().length
    });

    readonly $first = this.state.$first;
    readonly $rows = this.state.$rows;
    readonly $totalRecords = this.state.$totalRecords;

    // Computed signal for paginated content types
    readonly $paginatedContentTypes = computed(() => {
        const start = this.state.$first();
        const end = start + this.state.$rows();
        return this.$contentTypes().slice(start, end);
    });

    onPageChange(event: PaginatorState) {
        patchState(this.state, {
            $first: event.first,
            $rows: event.rows,
            $totalRecords: this.$contentTypes().length
        });
    }
}
