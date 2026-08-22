import { Observable, defer, shareReplay } from 'rxjs';

import { DestroyRef, Injectable, NgZone, OnDestroy, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { take } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotMessageService, DotSiteService } from '@dotcms/data-access';
import { DotCMSContentlet, DotGeneratedAIImage, DotSite } from '@dotcms/dotcms-models';
import {
    ASSET_PICKER_TITLE_KEYS,
    DotAIImagePromptComponent,
    DotAssetPickerComponent,
    DotAssetPickerMediaMode,
    buildAssetPickerConfig,
    buildAssetPickerDialogConfig
} from '@dotcms/ui';

import { AiContentDialogComponent } from '../components/ai-content-dialog/ai-content-dialog.component';
import { OVERLAY_ABOVE_FULLSCREEN_Z_INDEX } from '../config.utils';
import {
    insertDotAudioFromContentlet,
    insertDotImageFromContentlet,
    insertDotVideoFromContentlet
} from '../editor.utils';
import { EditorStore } from '../store/editor.store';

/** Inserts the picked contentlet as the node the media mode corresponds to. */
const INSERT_BY_MODE: Record<
    DotAssetPickerMediaMode,
    (editor: Editor, contentlet: DotCMSContentlet) => void
> = {
    image: insertDotImageFromContentlet,
    video: insertDotVideoFromContentlet,
    audio: insertDotAudioFromContentlet
};

/**
 * Owns every centered modal dialog in the editor — all opened via PrimeNG's
 * {@link DialogService.open}: AI content, AI image, and the image / video / audio pickers.
 * Sibling to {@link EditorPopoverService}, which owns caret-anchored popovers
 * (table, link, emoji, image-properties).
 *
 * Provided at the editor component scope so each editor instance has its own modal refs
 * and per-instance teardown via {@link ngOnDestroy}.
 */
@Injectable()
export class EditorModalService implements OnDestroy {
    private readonly zone = inject(NgZone);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly siteService = inject(DotSiteService);
    private readonly editorStore = inject(EditorStore);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * Live picker refs by media mode; an entry is cleared when its dialog closes or the service
     * tears down.
     */
    private pickerRefs = new Map<DotAssetPickerMediaMode, DynamicDialogRef>();

    /**
     * Modes whose site lookup is in flight. Separate from {@link pickerRefs} because the ref only
     * exists once the site resolves — without this, two fast clicks each pass the ref guard and open
     * two dialogs.
     */
    private pickerPending = new Set<DotAssetPickerMediaMode>();

    /**
     * The site the picker browses, resolved once per editor instance.
     *
     * Cached rather than re-fetched per open: the current site cannot change while the editor is
     * mounted, so three pickers asking separately would be three requests for one answer.
     * `refCount: false` keeps the value once the first subscriber has gone away.
     *
     * `defer` so nothing is requested until a picker is actually opened — most editing sessions never
     * open one — and so a failed lookup is retried on the next attempt instead of being cached as a
     * permanent failure (`shareReplay` resets itself on error).
     */
    private readonly currentSite$: Observable<DotSite> = defer(() =>
        this.siteService.getCurrentSite()
    ).pipe(take(1), shareReplay({ bufferSize: 1, refCount: false }));

    /**
     * Open state for the AI Image prompt modal. Tracking it as a signal lets other parts
     * of the editor know an AI Image dialog is active without poking the live ref.
     */
    readonly aiImageOpen = signal(false);

    /** Live PrimeNG dialog ref for the AI image prompt; cleared on close / destroy. */
    private aiImageDialogRef: DynamicDialogRef | null = null;

    /** Live PrimeNG dialog ref for the AI content prompt; cleared on close / destroy. */
    private aiContentDialogRef: DynamicDialogRef | null = null;

    /**
     * Opens {@link DotAssetPickerComponent} scoped to image-mime contentlets. On accept, inserts the
     * picked contentlet as a `dotImage` node at the editor's current selection.
     */
    openImagePicker(editor: Editor): void {
        this.openAssetPicker(editor, 'image');
    }

    /**
     * Opens {@link DotAssetPickerComponent} scoped to video-mime contentlets. On accept, inserts the
     * picked contentlet as a `dotVideo` node at the editor's current selection.
     */
    openVideoPicker(editor: Editor): void {
        this.openAssetPicker(editor, 'video');
    }

    /**
     * Opens {@link DotAssetPickerComponent} scoped to audio-mime contentlets. On accept, inserts the
     * picked contentlet as a `dotAudio` node at the editor's current selection.
     */
    openAudioPicker(editor: Editor): void {
        this.openAssetPicker(editor, 'audio');
    }

    /**
     * The one asset-picker flow, shared by image, video and audio — the same picker the Edit Content
     * File and Image fields open, so browsing for an asset looks the same everywhere.
     *
     * The site lookup makes this asynchronous, which is the only real wrinkle: the picker cannot be
     * configured without a `DotSite`, and there is nowhere in the editor that already holds one. A
     * mode with a lookup in flight or a dialog already open is skipped, so repeated clicks can never
     * stack two pickers.
     *
     * If the site cannot be resolved, nothing opens. A picker that can't browse anything is worse
     * than no picker, and there is nothing useful to say about it beyond that.
     */
    private openAssetPicker(editor: Editor, mode: DotAssetPickerMediaMode): void {
        if (this.pickerRefs.has(mode) || this.pickerPending.has(mode)) return;

        this.pickerPending.add(mode);

        this.currentSite$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (site) => {
                this.pickerPending.delete(mode);
                if (site) {
                    this.zone.run(() => this.mountAssetPicker(editor, mode, site));
                }
            },
            error: () => this.pickerPending.delete(mode)
        });
    }

    /** Opens the dialog for a resolved site. Split out so the lookup above stays readable. */
    private mountAssetPicker(editor: Editor, mode: DotAssetPickerMediaMode, site: DotSite): void {
        const ref = this.dialogService.open(
            DotAssetPickerComponent,
            buildAssetPickerDialogConfig(
                buildAssetPickerConfig({
                    mode,
                    site,
                    title: this.dotMessageService.get(ASSET_PICKER_TITLE_KEYS[mode]),
                    languageId: String(this.editorStore.languageId())
                }),
                // The fullscreen editor shell's `z-[9998]` backdrop would otherwise cover the modal.
                { baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX }
            )
        );

        if (!ref) {
            return;
        }

        this.pickerRefs.set(mode, ref);

        ref.onClose.subscribe((contentlet?: DotCMSContentlet) => {
            if (contentlet) {
                this.zone.run(() => INSERT_BY_MODE[mode](editor, contentlet));
            }
            this.pickerRefs.delete(mode);
        });
    }

    /**
     * Opens the AI Image prompt dialog ({@link DotAIImagePromptComponent} from `@dotcms/ui`).
     * On close, if the user accepted a generated image, inserts it as a `dotImage` node at
     * the editor's current selection. Closing without a selection (cancel/discard) is a
     * no-op other than clearing local state.
     */
    openAiImage(editor: Editor): void {
        if (this.aiImageDialogRef) return;

        this.zone.run(() => this.aiImageOpen.set(true));

        this.aiImageDialogRef = this.dialogService.open(DotAIImagePromptComponent, {
            header: this.dotMessageService.get('block-editor.extension.ai-image.dialog-title'),
            appendTo: 'body',
            // Modal must clear the fullscreen editor shell's `z-[9998]` backdrop.
            baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX,
            closeOnEscape: true,
            closable: true,
            dismissableMask: true,
            draggable: false,
            keepInViewport: false,
            resizable: false,
            modal: true,
            width: '90%',
            style: { 'max-width': '1040px' },
            data: { context: editor.getText() }
        });

        this.aiImageDialogRef?.onClose.subscribe((selectedImage?: DotGeneratedAIImage) => {
            const contentlet = selectedImage?.response?.contentlet;

            if (contentlet) {
                this.zone.run(() => insertDotImageFromContentlet(editor, contentlet));
            }
            this.aiImageDialogRef = null;
            this.zone.run(() => this.aiImageOpen.set(false));
        });
    }

    /**
     * Imperatively closes the AI Image prompt dialog. The dialog's own `onClose` subscription
     * resets the rest of the state, so we do not have to flip {@link aiImageOpen} here.
     */
    closeAiImage(): void {
        this.aiImageDialogRef?.close();
    }

    /**
     * Opens the AI Content prompt dialog ({@link AiContentDialogComponent}). On accept,
     * inserts the generated HTML as an `aiContent` node at the editor's current selection.
     * Cancel / Discard / Escape / X close with no value and no insertion.
     * Idempotent: a second call while the dialog is already open is a no-op.
     */
    openAiContent(editor: Editor): void {
        if (this.aiContentDialogRef) return;

        this.aiContentDialogRef = this.dialogService.open(AiContentDialogComponent, {
            header: this.dotMessageService.get('dot.block.editor.dialog.ai-content.header'),
            appendTo: 'body',
            // Modal must clear the fullscreen editor shell's `z-[9998]` backdrop.
            baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX,
            closeOnEscape: true,
            closable: true,
            // Match the original embedded behavior — clicking outside should NOT discard
            // an in-flight prompt or generated draft.
            dismissableMask: false,
            draggable: false,
            resizable: false,
            modal: true,
            width: '720px',
            style: { 'max-width': '90vw' }
        });

        this.aiContentDialogRef?.onClose.subscribe((html?: string) => {
            if (html) {
                this.zone.run(() => editor.chain().focus().insertContent(html).run());
            }
            this.aiContentDialogRef = null;
        });
    }

    ngOnDestroy(): void {
        for (const ref of this.pickerRefs.values()) {
            ref.close();
        }
        this.pickerRefs.clear();
        this.pickerPending.clear();
        this.aiImageDialogRef?.close();
        this.aiImageDialogRef = null;
        this.aiContentDialogRef?.close();
        this.aiContentDialogRef = null;
    }
}
