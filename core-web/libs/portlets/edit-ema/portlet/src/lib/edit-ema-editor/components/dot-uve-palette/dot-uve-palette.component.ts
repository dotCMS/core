import { patchState, signalState } from '@ngrx/signals';

import { ChangeDetectionStrategy, Component, inject, input, OnInit } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotCMSContentType, DotPagination } from '@dotcms/dotcms-models';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import {
    DotPageContentTypeParams,
    DotPageContentTypeService
} from './service/dot-page-contenttype.service';

interface PaletteState {
    contenttypes: DotCMSContentType[];
    pagination: DotPagination | null;
}

@Component({
    selector: 'dot-uve-palette',
    imports: [TabViewModule, DotUvePaletteListComponent],
    providers: [DotPageContentTypeService],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent implements OnInit {
    $languageId = input.required<number>({ alias: 'languageId' });
    $variantId = input.required<string>({ alias: 'variantId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });

    readonly #pageContentTypeService = inject(DotPageContentTypeService);

    readonly $state = signalState<PaletteState>({
        contenttypes: [],
        pagination: null
    });

    ngOnInit(): void {
        const params: DotPageContentTypeParams = {
            page: 1,
            per_page: 30,
            orderby: 'name',
            direction: 'ASC',
            pagePathOrId: this.$pagePath() === 'index' ? '/' : this.$pagePath(), // We need to fix this and use the `page.uriPath` or something
            language: this.$languageId().toString()
        };
        this.#pageContentTypeService.get(params).subscribe(({ contenttypes, pagination }) => {
            patchState(this.$state, { contenttypes, pagination });
        });
    }
}
