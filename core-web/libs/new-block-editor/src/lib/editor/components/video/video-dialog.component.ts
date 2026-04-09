import { computePosition, flip, shift } from '@floating-ui/dom';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    NgZone,
    afterRenderEffect,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { VideoDialogService } from './video-dialog.service';

import { DotCmsUploadService } from '../../services/dot-cms-upload.service';

type Tab = 'upload' | 'url';

@Component({
    selector: 'dot-block-editor-video-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule],
    host: {
        'aria-label': 'Insert video',
        class: 'absolute z-50 w-80 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg',
        '[style.display]': 'service.isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: `
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
        </div>

        <!-- Upload tab -->
        @if (activeTab() === 'upload') {
            <div class="p-4">
                <label
                    [class]="
                        'flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-6 transition-colors ' +
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
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
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
                (keydown.escape)="service.close()"
                (keydown.enter)="$event.preventDefault(); onInsertUrl()">
                <div class="flex flex-col gap-1">
                    <label for="vid-url" class="text-sm text-gray-700">URL</label>
                    <input
                        id="vid-url"
                        type="url"
                        [formControl]="urlControl"
                        placeholder="https://example.com/video.mp4"
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
    `
})
export class VideoDialogComponent {
    protected readonly service = inject(VideoDialogService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly document = inject(DOCUMENT);
    private readonly dotCmsUpload = inject(DotCmsUploadService);

    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);
    protected readonly activeTab = signal<Tab>('url');
    protected readonly uploading = signal(false);

    private previouslyFocused: HTMLElement | null = null;

    readonly urlControl = new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
    });

    readonly titleControl = new FormControl<string>('', { nonNullable: true });

    constructor() {
        effect((onCleanup) => {
            if (!this.service.isOpen()) return;

            this.previouslyFocused = this.document.activeElement as HTMLElement | null;

            const handleKeyDown = (event: KeyboardEvent) => {
                if (event.key === 'Escape') this.zone.run(() => this.service.close());
            };
            const handleMouseDown = (event: MouseEvent) => {
                if (!this.el.nativeElement.contains(event.target as Node)) {
                    this.zone.run(() => this.service.close());
                }
            };

            this.document.addEventListener('keydown', handleKeyDown);
            this.document.addEventListener('mousedown', handleMouseDown);
            onCleanup(() => {
                this.document.removeEventListener('keydown', handleKeyDown);
                this.document.removeEventListener('mousedown', handleMouseDown);
                this.previouslyFocused?.focus({ preventScroll: true });
                this.previouslyFocused = null;
            });
        });

        afterRenderEffect(() => {
            const isOpen = this.service.isOpen();
            const clientRectFn = this.service.clientRectFn();

            if (!isOpen || !clientRectFn) {
                untracked(() => {
                    this.positioned.set(false);
                    this.activeTab.set('url');
                    this.urlControl.reset('');
                    this.titleControl.reset('');
                });
                return;
            }

            const virtualRef = {
                getBoundingClientRect: () => clientRectFn() ?? new DOMRect()
            };

            computePosition(virtualRef, this.el.nativeElement, {
                placement: 'bottom-start',
                strategy: 'absolute',
                middleware: [flip(), shift({ padding: 8 })]
            }).then(({ x, y }) => {
                this.zone.run(() => {
                    untracked(() => {
                        this.floatX.set(x);
                        this.floatY.set(y);
                        this.positioned.set(true);
                    });
                });
                setTimeout(() => {
                    const firstInput = this.el.nativeElement.querySelector(
                        'input:not([type="file"]):not([type="checkbox"])'
                    ) as HTMLElement | null;
                    firstInput?.focus();
                }, 0);
            });
        });
    }

    tabClass(tab: Tab): string {
        const base =
            'flex flex-1 items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors';
        return this.activeTab() === tab
            ? `${base} border-indigo-500 text-indigo-600 bg-white`
            : `${base} border-transparent text-gray-500 hover:text-gray-700 bg-gray-50`;
    }

    async onFileChange(event: Event): Promise<void> {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (!file) return;

        this.uploading.set(true);
        try {
            const src = await this.dotCmsUpload.uploadVideo(file);
            const title = file.name.replace(/\.[^.]+$/, '');
            this.zone.run(() => this.service.insert(src, title));
        } catch (err) {
            console.error('Video upload failed', err);
        } finally {
            this.uploading.set(false);
        }
    }

    onInsertUrl(): void {
        if (this.urlControl.invalid) return;
        const title = this.titleControl.getRawValue().trim() || undefined;
        this.service.insert(this.urlControl.getRawValue(), title);
    }
}
