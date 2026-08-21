import { Observable, defer, shareReplay } from 'rxjs';
import { Editor } from 'tinymce';

import { DestroyRef, Injectable, NgZone, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DialogService } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import {
    DotMessageService,
    DotPropertiesService,
    DotSiteService,
    DotUploadFileService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotSite } from '@dotcms/dotcms-models';
import {
    ASSET_PICKER_TITLE_KEYS,
    DotAssetPickerComponent,
    buildAssetPickerConfig,
    buildAssetPickerDialogConfig
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
export class DotWysiwygPluginService {
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
     * Opens the shared asset picker, scoped to images — the same picker the Edit Content File and
     * Image fields and the Story Block use, so browsing for an asset looks the same everywhere.
     *
     * The site lookup makes this asynchronous: `DotAssetPickerComponent` cannot be configured
     * without a `DotSite` and this field holds none. That gap is why the busy flag exists rather than
     * a plain "is a dialog open" check — the ref does not exist yet while the lookup is running, so
     * two fast clicks on the toolbar button would otherwise stack two dialogs.
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

    /** Opens the picker for a resolved site. Split out so the lookup above stays readable. */
    private openImagePicker(editor: Editor, site: DotSite): void {
        const ref = this.dialogService.open(
            DotAssetPickerComponent,
            // Dialog flags live with the picker — they are its contract, not this field's taste.
            buildAssetPickerDialogConfig(
                buildAssetPickerConfig({
                    mode: 'image',
                    site,
                    title: this.dotMessageService.get(ASSET_PICKER_TITLE_KEYS.image),
                    languageId: this.pickerLanguageId()
                })
            )
        );

        ref.onClose.subscribe((asset: DotCMSContentlet) => {
            this.imagePickerBusy = false;

            if (asset) {
                editor.insertContent(formatDotImageNode(this.IMAGE_URL_PATTERN, asset));
            }

            // Return focus to the editor on every close (insert or dismiss via
            // X, Esc or overlay mask) so the user is never left without focus.
            editor.focus();
        });
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
