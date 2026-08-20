import { Observable, defer, shareReplay } from 'rxjs';
import { Editor } from 'tinymce';

import { DestroyRef, Injectable, NgZone, OnDestroy, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import {
    DotMessageService,
    DotPropertiesService,
    DotSiteService,
    DotUploadFileService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotSite } from '@dotcms/dotcms-models';
import {
    ASSET_PICKER_LAUNCHER,
    ASSET_PICKER_TITLE_KEYS,
    DotAssetSearchDialogComponent
} from '@dotcms/ui';

import { DEFAULT_IMAGE_URL_PATTERN, formatDotImageNode } from './utils/editor.utils';

import { DotEditContentStore } from '../../../store/edit-content.store';

/**
 * Service to initialize the plugins for the WYSIWYG editor
 *
 * @export
 * @class DotWysiwygPluginService
 */
@Injectable()
export class DotWysiwygPluginService implements OnDestroy {
    private readonly dialogService: DialogService = inject(DialogService);
    private readonly dotUploadFileService: DotUploadFileService = inject(DotUploadFileService);
    private readonly dotPropertiesService: DotPropertiesService = inject(DotPropertiesService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly siteService = inject(DotSiteService);
    private readonly ngZone: NgZone = inject(NgZone);

    /**
     * Optional for the same reason the File field's is: this service is also constructed by hosts
     * that are not the Edit Content shell (legacy Dojo pages), where the store does not exist.
     */
    private readonly editContentStore = inject(DotEditContentStore, { optional: true });

    /**
     * Present only in the Angular Edit Content, the sole host the new AssetPicker was built for.
     * Absent in the legacy Dojo pages, where *Add image* falls back to
     * {@link DotAssetSearchDialogComponent} — the dialog this button opened before the picker
     * existed.
     */
    private readonly assetPickerLauncher = inject(ASSET_PICKER_LAUNCHER, { optional: true });

    /**
     * Live ref for whichever picker is open, so the field can close it on teardown. PrimeNG does not
     * do this for us — `DialogService` has no `ngOnDestroy` — so without this a dialog outlives the
     * field that opened it.
     */
    private pickerRef: DynamicDialogRef | null = null;

    private IMAGE_URL_PATTERN = DEFAULT_IMAGE_URL_PATTERN;

    private readonly destroyRef$ = inject(DestroyRef);

    /** True while a picker is open or its site lookup is in flight — see {@link dotImageDialog}. */
    private imagePickerBusy = false;

    /**
     * Site the picker browses, resolved once per editor instance — it cannot change while the field
     * is mounted. `defer` keeps it lazy (most editing sessions never insert an image) and lets a
     * failed lookup be retried on the next click instead of being cached as a permanent failure.
     */
    private readonly currentSite$: Observable<DotSite> = defer(() =>
        this.siteService.getCurrentSite()
    ).pipe(take(1), shareReplay({ bufferSize: 1, refCount: false }));

    constructor() {
        this.dotPropertiesService
            .getKey('WYSIWYG_IMAGE_URL_PATTERN')
            .pipe(
                takeUntilDestroyed(this.destroyRef$),
                filter((value): value is string => typeof value === 'string' && !!value)
            )
            .subscribe((value) => (this.IMAGE_URL_PATTERN = value));
    }

    /**
     * Initialize the plugins for the WYSIWYG editor
     * It should be called in the setup of the editor
     *
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    initializePlugins(editor: Editor): void {
        this.dotImagePlugin(editor);
    }

    /**
     * Add the image plugin to the editor
     *
     * @private
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    private dotImagePlugin(editor: Editor): void {
        editor.ui.registry.addButton('dotAddImage', {
            icon: 'image',
            // TinyMCE renders `tooltip` as both `title` and `aria-label` (silver theme's
            // `getTooltipAttributes`). Without it this icon-only button has no accessible name at
            // all — nothing for a screen reader to announce and nothing to hover.
            tooltip: this.dotMessageService.get('insert-image'),
            onAction: () => this.dotImageDialog(editor)
        });
        this.handleImageDrop(editor);
    }

    /**
     * Opens a picker scoped to images — *which* one is the host's call, not this field's.
     *
     * With {@link assetPickerLauncher} present (the Angular Edit Content) it is the shared
     * AssetPicker, the same one the File and Image fields and the Story Block open. Without it — the
     * legacy Dojo pages this service documents itself as constructible from — it is
     * {@link DotAssetSearchDialogComponent}, the dialog this button opened before the AssetPicker
     * existed. The old editor was never designed for the new one.
     *
     * Only the AssetPicker is asynchronous: it cannot be configured without a `DotSite` and this
     * field holds none. That gap is why the busy flag exists rather than a plain "is a dialog open"
     * check — the ref does not exist yet while the lookup is running, so two fast clicks on the
     * toolbar button would otherwise stack two dialogs. The flag guards the legacy path too, which
     * needs no site and so never pays for the lookup.
     *
     * If the site cannot be resolved, nothing opens: a picker that can't browse anything is worse
     * than no picker.
     *
     * @private
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    private dotImageDialog(editor: Editor): void {
        if (this.imagePickerBusy) {
            return;
        }

        this.imagePickerBusy = true;

        if (!this.assetPickerLauncher) {
            this.ngZone.run(() => this.openLegacyImageDialog(editor));

            return;
        }

        this.currentSite$.pipe(takeUntilDestroyed(this.destroyRef$)).subscribe({
            next: (site) => {
                if (!site) {
                    this.imagePickerBusy = false;

                    return;
                }

                this.ngZone.run(() => this.openImagePicker(editor, site));
            },
            error: () => (this.imagePickerBusy = false)
        });
    }

    /** Opens the AssetPicker for a resolved site. Split out so the lookup above stays readable. */
    private openImagePicker(editor: Editor, site: DotSite): void {
        this.trackImagePicker(
            editor,
            this.assetPickerLauncher.open(
                // The launcher borrows this service's `DialogService` so the picker stays scoped to
                // this field — see `ASSET_PICKER_LAUNCHER`.
                this.dialogService,
                {
                    mode: 'image',
                    site,
                    title: this.dotMessageService.get(ASSET_PICKER_TITLE_KEYS.image),
                    languageId: this.pickerLanguageId()
                }
            )
        );
    }

    /**
     * Opens the pre-AssetPicker image search dialog — what the legacy hosts have always shown.
     * Synchronous, because it browses without a `DotSite`.
     */
    private openLegacyImageDialog(editor: Editor): void {
        this.trackImagePicker(
            editor,
            this.dialogService.open(DotAssetSearchDialogComponent, {
                header: 'Insert Image',
                width: '800px',
                height: '500px',
                contentStyle: { padding: 0 },
                closable: true,
                closeOnEscape: true,
                dismissableMask: true,
                data: {
                    assetType: 'image'
                }
            })
        );
    }

    /**
     * Holds the live ref and inserts whatever the dialog closes with. Shared by both pickers: they
     * differ in what the user browses, not in what a selection means.
     */
    private trackImagePicker(editor: Editor, ref: DynamicDialogRef): void {
        this.pickerRef = ref;

        ref.onClose.subscribe((asset: DotCMSContentlet) => {
            this.imagePickerBusy = false;
            this.pickerRef = null;

            if (asset) {
                editor.insertContent(formatDotImageNode(this.IMAGE_URL_PATTERN, asset));
            }

            // Return focus to the editor on every close (insert or dismiss via
            // X, Esc or overlay mask) so the user is never left without focus.
            editor.focus();
        });
    }

    /**
     * Closes an open picker when the field is torn down. PrimeNG's `DialogService` has no
     * `ngOnDestroy`, so nothing else would.
     */
    ngOnDestroy(): void {
        this.pickerRef?.close();
        this.pickerRef = null;
    }

    /**
     * Locale to pre-select in the picker, from the contentlet being edited. `undefined` when there is
     * no Edit Content store to ask, which leaves the picker unfiltered by locale.
     */
    private pickerLanguageId(): string | undefined {
        const languageId = this.editContentStore?.currentLocale()?.id;

        return languageId ? String(languageId) : undefined;
    }

    /**
     *  Handle the drop event in the editor
     *
     * @private
     * @param {Editor} editor
     * @memberof DotWysiwygPluginService
     */
    private handleImageDrop(editor: Editor) {
        editor.on('drop', (event) => {
            const file = event.dataTransfer.files[0];

            // Check if the file is an image
            if (!file.type.includes('image')) {
                return;
            }

            event.preventDefault();
            event.stopImmediatePropagation();
            event.stopPropagation();

            this.dotUploadFileService
                .publishContent({
                    data: file
                })
                .subscribe((contentlets) => {
                    const data = contentlets[0];
                    const asset = data[Object.keys(data)[0]];
                    editor.insertContent(formatDotImageNode(this.IMAGE_URL_PATTERN, asset));
                });
        });
    }
}
