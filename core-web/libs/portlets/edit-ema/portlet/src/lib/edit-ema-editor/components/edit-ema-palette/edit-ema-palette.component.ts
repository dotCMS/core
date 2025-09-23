import { Subject } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    OnInit,
    effect,
    inject,
    input,
    untracked
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { DotESContentService } from '@dotcms/data-access';
import { DotCMSPageAssetContainers } from '@dotcms/types';

import { EditEmaPaletteContentTypeComponent } from './components/edit-ema-palette-content-type/edit-ema-palette-content-type.component';
import { EditEmaPaletteContentletsComponent } from './components/edit-ema-palette-contentlets/edit-ema-palette-contentlets.component';
import { DotPaletteStore, PALETTE_TYPES } from './store/edit-ema-palette.store';

@Component({
    selector: 'dot-edit-ema-palette',
    templateUrl: './edit-ema-palette.component.html',
    styleUrls: ['./edit-ema-palette.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [AsyncPipe, EditEmaPaletteContentTypeComponent, EditEmaPaletteContentletsComponent],
    providers: [DotESContentService, DotPaletteStore]
})
export class EditEmaPaletteComponent implements OnInit, OnDestroy {
    $languageId = input.required<number>({ alias: 'languageId' });
    $variantId = input.required<string>({ alias: 'variantId' });
    $containers = input.required<DotCMSPageAssetContainers>({ alias: 'containers' });

    readonly #store = inject(DotPaletteStore);
    private destroy$ = new Subject<void>();

    protected readonly vm$ = this.#store.vm$;
    protected readonly $isContentTypeView = toSignal(this.#store.isContentTypeView$);
    protected readonly PALETTE_TYPES_ENUM = PALETTE_TYPES;

    readonly UPDATE_CONTENTLETS_EFFECT = effect(() => {
        const languageId = this.$languageId();
        const variantId = this.$variantId();

        if (this.$isContentTypeView()) {
            return;
        }

        untracked(() => this.#store.refreshContentlets({ languageId, variantId }));
    });

    ngOnInit() {
        this.#store.loadAllowedContentTypes({ containers: this.$containers() });
    }

    /**
     * Shows contentlets from a specific content type.
     *
     * @param contentTypeName - The name of the content type.
     */
    showContentletsFromContentType(contentTypeName: string) {
        this.#store.loadContentlets({
            filter: '',
            languageId: this.$languageId().toString(),
            contenttypeName: contentTypeName,
            variantId: this.$variantId()
        });
    }

    /**
     * Shows the content types in the palette.
     */
    showContentTypes() {
        this.#store.resetContentlets();
    }

    /**
     *
     *
     * @param {*} { contentTypeVarName, page }
     * @memberof EditEmaPaletteComponent
     */
    onPaginate({ contentTypeVarName, page }) {
        this.#store.loadContentlets({
            filter: '',
            languageId: this.$languageId().toString(),
            contenttypeName: contentTypeVarName,
            page: page,
            variantId: this.$variantId()
        });
    }

    loadContentTypes(filter: string, allowedContent: string[]) {
        this.#store.loadContentTypes({
            filter,
            allowedContent
        });
    }

    loadContentlets(filter: string, currentContentType: string) {
        this.#store.loadContentlets({
            filter,
            contenttypeName: currentContentType,
            languageId: this.$languageId().toString(),
            variantId: this.$variantId()
        });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
