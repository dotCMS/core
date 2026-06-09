import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotESContentService } from '@dotcms/data-access';
import {
    DotPaletteListStore,
    DotUvePaletteListComponent,
    DotUVEPaletteListTypes
} from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveNavigationService } from '../../../shared/services/dot-content-drive-navigation.service';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

/**
 * Content Drive "New" dialog body: reuses the UVE palette list in selection mode to let
 * the user pick a content type, then a Create button navigates to the content editor.
 *
 * The base type(s) shown are encoded by the `listType` input (e.g. ALL_CONTENT_TYPES or a
 * single base type), set by the toolbar menu option that opened the dialog.
 */
@Component({
    selector: 'dot-content-drive-dialog-content-type-selector',
    imports: [DotUvePaletteListComponent, ButtonModule, DotMessagePipe],
    // DotPaletteListStore injects DotESContentService (not providedIn root); provide it here
    // since the store is created in this injector. In UVE it comes from the route providers.
    providers: [DotPaletteListStore, DotESContentService],
    templateUrl: './dot-content-drive-dialog-content-type-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-[min(80vh,46rem)]' }
})
export class DotContentDriveDialogContentTypeSelectorComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #navigationService = inject(DotContentDriveNavigationService);

    /** Palette list type that encodes which base type(s) to display. */
    $listType = input.required<DotUVEPaletteListTypes>({ alias: 'listType' });

    /** Currently selected content type variable (enables the Create button). */
    protected readonly $selectedVariable = signal<string | null>(null);
    protected readonly $canCreate = computed(() => !!this.$selectedVariable());

    protected onSelect(variable: string): void {
        this.$selectedVariable.set(variable);
    }

    protected onCreate(): void {
        const variable = this.$selectedVariable();
        if (!variable) {
            return;
        }

        this.#store.closeDialog();
        this.#navigationService.createContent(variable);
    }

    protected onCancel(): void {
        this.#store.closeDialog();
    }
}
