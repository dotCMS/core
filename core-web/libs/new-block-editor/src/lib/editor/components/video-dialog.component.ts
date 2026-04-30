import {
    ChangeDetectionStrategy,
    Component,
    NgZone,
    OnDestroy,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotBrowserSelectorComponent } from '@dotcms/ui';

import { EditorDialogComponent } from './editor-dialog.component';

import { buildBrowserSelectorConfig } from '../config.utils';
import { insertDotVideoFromContentlet } from '../editor.utils';
import { DOT_VIDEO_NODE_NAME } from '../extensions/nodes/video.extension';
import { DotUploadService } from '../services/dot-upload.service';
import { EditorDialogManagerService } from '../services/editor-dialog-manager.service';

type Tab = 'upload' | 'url' | 'dotcms';

// Matches youtube.com/watch?v=…, youtu.be/…, and the youtube-nocookie variant.
const YOUTUBE_URL_REGEX = /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube-nocookie\.com\/embed\/)/i;

@Component({
    selector: 'dot-video-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, EditorDialogComponent],
    template: `
        <dot-editor-dialog dialogId="video">
            <div
                aria-label="Insert video"
                class="w-[32rem] max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <!-- Tabs -->
                <div class="flex border-b border-gray-200" role="tablist">
                    <button
                        type="button"
                        role="tab"
                        [attr.aria-selected]="activeTab() === 'upload'"
                        [class]="tabClass('upload')"
                        (mousedown)="$event.preventDefault(); activeTab.set('upload')">
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            class="h-4 w-4"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            aria-hidden="true">
                            <path
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                stroke-width="2"
                                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                        </svg>
                        Upload video
                    </button>
                    <button
                        type="button"
                        role="tab"
                        [attr.aria-selected]="activeTab() === 'url'"
                        [class]="tabClass('url')"
                        (mousedown)="$event.preventDefault(); activeTab.set('url')">
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            class="h-4 w-4"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            aria-hidden="true">
                            <path
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                stroke-width="2"
                                d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                        </svg>
                        Video URL
                    </button>
                    <button
                        type="button"
                        role="tab"
                        [attr.aria-selected]="activeTab() === 'dotcms'"
                        [class]="tabClass('dotcms')"
                        data-testid="video-dialog-tab-dotcms"
                        (mousedown)="$event.preventDefault(); onSelectDotcmsTab()">
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            class="h-4 w-4 shrink-0"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            aria-hidden="true">
                            <path
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                stroke-width="2"
                                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        dotCMS
                    </button>
                </div>

                <!-- Upload tab -->
                @if (activeTab() === 'upload') {
                    <div class="p-4">
                        <label
                            [class]="
                                'flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 transition-colors ' +
                                (uploading()
                                    ? 'border-indigo-300 bg-indigo-50 cursor-wait pointer-events-none'
                                    : 'border-gray-300 cursor-pointer hover:border-indigo-400 hover:bg-indigo-50')
                            "
                            for="vid-upload">
                            @if (uploading()) {
                                <svg
                                    class="h-8 w-8 animate-spin text-indigo-400"
                                    xmlns="http://www.w3.org/2000/svg"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    aria-hidden="true">
                                    <circle
                                        class="opacity-25"
                                        cx="12"
                                        cy="12"
                                        r="10"
                                        stroke="currentColor"
                                        stroke-width="4" />
                                    <path
                                        class="opacity-75"
                                        fill="currentColor"
                                        d="M4 12a8 8 0 018-8v8z" />
                                </svg>
                                <span class="text-sm text-indigo-600">Uploading…</span>
                            } @else {
                                <svg
                                    xmlns="http://www.w3.org/2000/svg"
                                    class="h-8 w-8 text-gray-400"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                    aria-hidden="true">
                                    <path
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                        stroke-width="1.5"
                                        d="M15 10l4.553-2.277A1 1 0 0121 8.677v6.646a1 1 0 01-1.447.894L15 14M3 8a2 2 0 012-2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V8z" />
                                </svg>
                                <span class="text-sm text-gray-600">Click to upload</span>
                                <span class="text-xs text-gray-400">MP4, MOV, WebM</span>
                            }
                            <input
                                id="vid-upload"
                                type="file"
                                accept="video/*"
                                class="sr-only"
                                [disabled]="uploading()"
                                (change)="onFileChange($event)" />
                        </label>
                    </div>
                }

                <!-- URL tab -->
                @if (activeTab() === 'url') {
                    <div
                        class="p-4 flex flex-col gap-3"
                        (keydown.enter)="$event.preventDefault(); onInsertUrl()">
                        <div class="flex flex-col gap-1">
                            <label for="vid-url" class="text-sm text-gray-700">URL</label>
                            <input
                                id="vid-url"
                                type="url"
                                [formControl]="urlControl"
                                placeholder="https://example.com/video.mp4 or YouTube URL"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>
                        <div class="flex flex-col gap-1">
                            <label for="vid-title" class="text-sm text-gray-700">
                                Title
                                <span class="text-gray-400 text-xs">(optional)</span>
                            </label>
                            <input
                                id="vid-title"
                                type="text"
                                [formControl]="titleControl"
                                placeholder="Video title"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>
                        <div class="flex justify-end">
                            <button
                                type="button"
                                (mousedown)="$event.preventDefault(); onInsertUrl()"
                                [disabled]="urlControl.invalid"
                                class="rounded bg-indigo-500 px-4 py-1.5 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                                Insert
                            </button>
                        </div>
                    </div>
                }

                @if (activeTab() === 'dotcms') {
                    <div class="flex flex-col items-center gap-4 p-6">
                        <p class="text-center text-sm text-gray-600">
                            Browse videos stored in dotCMS — folders, filters, and previews.
                        </p>
                        <button
                            type="button"
                            data-testid="dotcms-video-open-browser"
                            (mousedown)="$event.preventDefault(); openDotcmsBrowser()"
                            class="rounded bg-indigo-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400">
                            Open dotCMS video picker
                        </button>
                    </div>
                }
            </div>
        </dot-editor-dialog>
    `
})
export class VideoDialogComponent implements OnDestroy {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorDialogManagerService);
    private readonly zone = inject(NgZone);
    private readonly dotUpload = inject(DotUploadService);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly activeTab = signal<Tab>('url');
    protected readonly uploading = signal(false);

    readonly urlControl = new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
    });
    readonly titleControl = new FormControl<string>('', { nonNullable: true });

    /** Live PrimeNG dialog ref for the dotCMS video browser-selector; cleared on close / destroy. */
    private dotcmsVideoPickerRef: DynamicDialogRef | null = null;

    constructor() {
        effect(() => {
            if (!this.manager.isOpen('video')) {
                untracked(() => {
                    this.activeTab.set('url');
                    this.urlControl.reset('');
                    this.titleControl.reset('');
                    this.uploading.set(false);
                });
            }
        });
    }

    tabClass(tab: Tab): string {
        const base =
            'flex min-w-0 flex-1 items-center justify-center gap-1.5 px-2 py-2.5 text-xs font-medium border-b-2 transition-colors sm:gap-2 sm:px-3 sm:text-sm';
        return this.activeTab() === tab
            ? `${base} border-indigo-500 text-indigo-600 bg-white`
            : `${base} border-transparent text-gray-500 hover:text-gray-700 bg-gray-50`;
    }

    onSelectDotcmsTab(): void {
        this.activeTab.set('dotcms');
    }

    /**
     * Closes this caret-anchored popover and hands off to the centered dotCMS
     * browser-selector ({@link DotBrowserSelectorComponent} from `@dotcms/ui`). Mirrors
     * `<dot-image-dialog>`'s flow with `mimeTypes: ['video']`. On accept, inserts the
     * picked contentlet as a `dotVideo` node via {@link insertDotVideoFromContentlet}.
     */
    openDotcmsBrowser(): void {
        if (this.dotcmsVideoPickerRef) return;

        // Dismiss the popover-anchored video dialog before opening the centered modal so
        // they don't stack on top of each other.
        this.manager.close();

        const editor = this.editor();
        this.dotcmsVideoPickerRef = this.dialogService.open(
            DotBrowserSelectorComponent,
            buildBrowserSelectorConfig({
                header: this.dotMessageService.get(
                    'block-editor.extension.video.dotcms.dialog-title'
                ),
                mimeTypes: ['video']
            })
        );

        this.dotcmsVideoPickerRef.onClose.subscribe((contentlet?: DotCMSContentlet) => {
            if (contentlet) {
                this.zone.run(() => insertDotVideoFromContentlet(editor, contentlet));
            }
            this.dotcmsVideoPickerRef = null;
        });
    }

    ngOnDestroy(): void {
        this.dotcmsVideoPickerRef?.close();
        this.dotcmsVideoPickerRef = null;
    }

    async onFileChange(event: Event): Promise<void> {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (!file) return;

        this.uploading.set(true);
        try {
            const { src, data } = await this.dotUpload.uploadVideo(file);
            const title = file.name.replace(/\.[^.]+$/, '');
            this.zone.run(() => {
                this.editor()
                    .chain()
                    .focus()
                    .insertContent({
                        type: DOT_VIDEO_NODE_NAME,
                        attrs: { src, title: title ?? null, data }
                    })
                    .run();
                this.manager.close();
            });
        } catch (err) {
            console.error('Video upload failed', err);
        } finally {
            this.uploading.set(false);
        }
    }

    onInsertUrl(): void {
        if (this.urlControl.invalid) return;
        const url = this.urlControl.getRawValue();
        const title = this.titleControl.getRawValue().trim() || undefined;
        const editor = this.editor();

        // YouTube links use TipTap's youtube extension (renders an iframe embed).
        // Anything else gets the dotVideo <video> node.
        if (YOUTUBE_URL_REGEX.test(url)) {
            editor.chain().focus().setYoutubeVideo({ src: url }).run();
        } else {
            editor
                .chain()
                .focus()
                .insertContent({
                    type: DOT_VIDEO_NODE_NAME,
                    attrs: { src: url, title: title ?? null, data: null }
                })
                .run();
        }

        this.manager.close();
    }
}
