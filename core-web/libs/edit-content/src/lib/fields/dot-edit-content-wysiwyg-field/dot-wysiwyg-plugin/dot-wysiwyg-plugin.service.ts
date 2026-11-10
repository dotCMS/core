import { Observable, Subscription, defer, shareReplay } from 'rxjs';
import { Editor, RawEditorOptions } from 'tinymce';

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

import {
    DEFAULT_IMAGE_URL_PATTERN,
    formatDotImageNode,
    replaceURLPattern
} from './utils/editor.utils';

import { DotEditContentStore } from '../../../store/edit-content.store';

/** TinyMCE's `file_picker_callback`: hands a URL (and field values) back to the open dialog. */
type FilePickerCallback = Parameters<RawEditorOptions['file_picker_callback']>[0];

/** z-index of TinyMCE's dialog layer (`.tox-tinymce-aux`), which a picker must open above. */
const TINYMCE_DIALOG_Z_INDEX = 1300;

/** How the legacy editor's link picker addressed a dotAsset; file assets use their path. */
const DOT_ASSET_FILE_URL_PATTERN = '/dA/{shortyId}/{name}';

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
     * Anywhere else *Add image* falls back to {@link DotAssetSearchDialogComponent}, the dialog this
     * button opened before the picker existed.
     *
     * No legacy host is currently known to construct this service: the Dojo editor renders WYSIWYG
     * as a plain textarea plus JSP-side TinyMCE, and no Angular custom element exists for the field.
     * The fallback is therefore a consistency guard — so all three asset-selection entry points
     * behave identically — rather than a live regression path. See the Assumptions section of
     * `specs/37132-picker-per-host/spec.md`.
     */
    private readonly assetPickerLauncher = inject(ASSET_PICKER_LAUNCHER, { optional: true });

    /**
     * Live ref for whichever picker is open, so the field can close it on teardown. PrimeNG does not
     * do this for us — `DialogService` has no `ngOnDestroy` — so without this a dialog outlives the
     * field that opened it.
     */
    private pickerRef: DynamicDialogRef | null = null;

    /**
     * Subscription to the live picker's `onClose`, held so teardown can cancel it *before* closing
     * the dialog — see {@link ngOnDestroy}.
     */
    private pickerCloseSub: Subscription | null = null;

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
     * TinyMCE options that put a browse button at the end of the Source field of Insert › Image
     * and the URL field of Insert › Link, the way the legacy editor had one.
     *
     * The button opens the same AssetPicker as the File and Image fields — `image` mode from the
     * image dialog, `file` mode from the link dialog — and hands the chosen asset's URL back to
     * that dialog instead of inserting anything, so the editor still decides what to do with it.
     *
     * Empty without {@link assetPickerLauncher}: no other host has a picker to offer, and TinyMCE
     * only renders the button when a callback is set. `file_picker_types` comes back with it so a
     * system-wide or per-field config can still narrow it (e.g. to `"image"`).
     */
    filePickerOptions(): Pick<RawEditorOptions, 'file_picker_types' | 'file_picker_callback'> {
        if (!this.assetPickerLauncher) {
            return {};
        }

        return {
            file_picker_types: 'file image',
            file_picker_callback: (callback, _value, meta) =>
                this.openFilePicker(callback, meta?.filetype === 'image' ? 'image' : 'file')
        };
    }

    /**
     * Opens the AssetPicker for one of TinyMCE's dialogs and passes the selection to `callback`.
     *
     * Shares {@link imagePickerBusy} and the tracked ref with the toolbar button: the field shows
     * one picker at a time, and teardown closes whichever is open.
     */
    private openFilePicker(callback: FilePickerCallback, mode: 'image' | 'file'): void {
        if (this.imagePickerBusy) {
            return;
        }

        this.imagePickerBusy = true;

        this.currentSite$.pipe(takeUntilDestroyed(this.destroyRef$)).subscribe({
            next: (site) => {
                if (!site) {
                    this.imagePickerBusy = false;

                    return;
                }

                this.ngZone.run(() =>
                    this.releaseBusyIfOpenThrows(() =>
                        this.trackPicker(
                            this.assetPickerLauncher.open(
                                this.dialogService,
                                {
                                    mode,
                                    site,
                                    title: this.dotMessageService.get(
                                        ASSET_PICKER_TITLE_KEYS[mode]
                                    ),
                                    languageId: this.pickerLanguageId()
                                },
                                // TinyMCE's dialog layer sits at z-index 1300 and PrimeNG stacks
                                // from 1100, so without this the picker opens behind the dialog
                                // that asked for it.
                                { baseZIndex: TINYMCE_DIALOG_Z_INDEX }
                            ),
                            (asset) => {
                                if (!asset) {
                                    return;
                                }

                                // TinyMCE writes these into its own fields (attributes, not
                                // markup), so the title needs no escaping here.
                                callback(
                                    this.assetUrl(asset, mode),
                                    mode === 'image' ? { alt: asset.title } : { text: asset.title }
                                );
                            }
                        )
                    )
                );
            },
            error: () => (this.imagePickerBusy = false)
        });
    }

    /**
     * URL a dialog gets for a picked asset.
     *
     * Images use the same `WYSIWYG_IMAGE_URL_PATTERN` as the toolbar button, so both entry points
     * produce the same `src`. Files follow the legacy editor's link picker: a file asset links to its
     * path, a dotAsset to its `/dA/` URL.
     */
    private assetUrl(asset: DotCMSContentlet, mode: 'image' | 'file'): string {
        if (mode === 'image') {
            return replaceURLPattern(this.IMAGE_URL_PATTERN, asset);
        }

        return asset.baseType === 'FILEASSET' && asset.path
            ? asset.path
            : replaceURLPattern(DOT_ASSET_FILE_URL_PATTERN, asset);
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
     * AssetPicker, the same one the File and Image fields and the Story Block open. Without it, it
     * is {@link DotAssetSearchDialogComponent}, the dialog this button opened before the AssetPicker
     * existed — the old editor was never designed for the new one. See {@link assetPickerLauncher}
     * for why that branch is a guard rather than a path any known host takes today.
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
            this.ngZone.run(() =>
                this.releaseBusyIfOpenThrows(() => this.openLegacyImageDialog(editor))
            );

            return;
        }

        this.currentSite$.pipe(takeUntilDestroyed(this.destroyRef$)).subscribe({
            next: (site) => {
                if (!site) {
                    this.imagePickerBusy = false;

                    return;
                }

                this.ngZone.run(() =>
                    this.releaseBusyIfOpenThrows(() => this.openImagePicker(editor, site))
                );
            },
            error: () => (this.imagePickerBusy = false)
        });
    }

    /**
     * Runs an open, releasing {@link imagePickerBusy} if it throws.
     *
     * Nothing else can release it in that case: the flag is cleared by the picker's `onClose`, and
     * an open that threw never got as far as wiring one — so the toolbar button would stay dead for
     * the rest of the session. The error is deliberately rethrown rather than swallowed; a button
     * that silently does nothing is harder to diagnose than one that leaves a stack trace.
     */
    private releaseBusyIfOpenThrows(open: () => void): void {
        try {
            open();
        } catch (error) {
            this.imagePickerBusy = false;

            throw error;
        }
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
        this.trackPicker(ref, (asset) => {
            if (asset) {
                editor.insertContent(formatDotImageNode(this.IMAGE_URL_PATTERN, asset));
            }

            // Return focus to the editor on every close (insert or dismiss via
            // X, Esc or overlay mask) so the user is never left without focus.
            editor.focus();
        });
    }

    /**
     * Holds the live ref so teardown can close it, frees the busy flag on close, then hands the
     * selection (or `undefined` on dismiss) to `onClose`. Shared by every picker this field opens.
     */
    private trackPicker(
        ref: DynamicDialogRef,
        onClose: (asset: DotCMSContentlet | undefined) => void
    ): void {
        this.pickerRef = ref;

        this.pickerCloseSub = ref.onClose.subscribe((asset: DotCMSContentlet) => {
            this.imagePickerBusy = false;
            this.pickerRef = null;
            onClose(asset);
        });
    }

    /**
     * Closes an open picker when the field is torn down. PrimeNG's `DialogService` has no
     * `ngOnDestroy`, so nothing else would.
     *
     * The unsubscribe has to come **first**. `DynamicDialogRef.close()` pushes through `onClose`
     * synchronously (`_onClose.next(result)` — the close animation only gates `destroy()`), so
     * leaving the subscription live would run the close handler inline, right here, and call
     * `editor.focus()` on a TinyMCE instance `DotWysiwygTinymceComponent.ngOnDestroy` may already
     * have `remove()`d. `takeUntilDestroyed` would not fix that: it makes no ordering guarantee
     * against this hook. Suppressing the handler also means nothing else clears the busy flag, so
     * teardown does it.
     */
    ngOnDestroy(): void {
        this.pickerCloseSub?.unsubscribe();
        this.pickerCloseSub = null;
        this.imagePickerBusy = false;
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
