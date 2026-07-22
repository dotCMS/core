import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotESContentService } from '@dotcms/data-access';
import {
    DOT_PALETTE_PERSIST_PREFERENCES,
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
    // Persistence off: this transient picker must not read/overwrite the UVE palette's saved
    // view-mode/sort preferences (it's cards-only with ephemeral sort).
    providers: [
        DotPaletteListStore,
        DotESContentService,
        { provide: DOT_PALETTE_PERSIST_PREFERENCES, useValue: false }
    ],
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
        this.#navigationService.createContent(variable, this.#getCurrentFolder());
    }

    /**
     * Resolves the folder the user is currently browsing so the new content lands there.
     * - `folderPath` (`hostname/path`) pre-selects the Host/Folder field in the new editor.
     * - `folderInode` pre-selects the target folder in the legacy editor.
     * At the site root both fall back to the current site (empty path / no inode).
     */
    #getCurrentFolder(): { folderPath?: string; folderInode?: string } {
        const hostname = this.#store.currentSite()?.hostname;
        const path = this.#store.path();
        const inode = this.#store.selectedNode()?.data?.inode;

        return {
            folderPath: hostname ? `${hostname}${path ?? ''}` : undefined,
            folderInode: inode || undefined
        };
    }

    protected onCancel(): void {
        this.#store.closeDialog();
    }
}
