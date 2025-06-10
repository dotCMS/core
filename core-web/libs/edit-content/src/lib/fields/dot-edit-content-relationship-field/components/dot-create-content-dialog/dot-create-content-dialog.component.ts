import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    OnInit,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentType, DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-create-content-dialog',
    standalone: true,
    imports: [],
    templateUrl: './dot-create-content-dialog.component.html',
    styleUrls: ['./dot-create-content-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCreateContentDialogComponent implements OnInit {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig);
    readonly #destroyRef = inject(DestroyRef);
    readonly #sanitizer = inject(DomSanitizer);

    readonly legacyIframe = viewChild<any>('legacyIframe');

    readonly mode = signal<'create' | 'create-legacy'>('create');
    readonly contentType = signal<DotCMSContentType | null>(null);
    readonly iframeUrl = signal<string | null>(null);
    readonly sanitizedIframeUrl = signal<SafeResourceUrl | null>(null);

    ngOnInit() {
        const data = this.#dialogConfig.data;

        if (data) {
            this.mode.set(data.mode || 'create');
            this.contentType.set(data.contentType);

            if (data.iframeUrl) {
                this.iframeUrl.set(data.iframeUrl);
                this.sanitizedIframeUrl.set(
                    this.#sanitizer.bypassSecurityTrustResourceUrl(data.iframeUrl)
                );
            }
        }

        // Set up iframe event listeners for legacy mode
        if (this.mode() === 'create-legacy') {
            this.#setupLegacyIframeListeners();
        }
    }

    onIframeLoad() {
        // Focus the iframe content when it loads
        const iframe = this.legacyIframe();
        if (iframe?.nativeElement?.contentWindow) {
            iframe.nativeElement.contentWindow.focus();
        }
    }

    #setupLegacyIframeListeners() {
        // Listen for custom events from the legacy iframe
        // This is where we would capture content creation events

        // For now, we'll simulate a successful content creation after 3 seconds
        // In a real implementation, this would listen to iframe postMessage events
        setTimeout(() => {
            // Simulate successful content creation
            const mockCreatedContent: Partial<DotCMSContentlet> = {
                inode: 'mock-inode-' + Date.now(),
                title: 'New Content Item',
                contentType: this.contentType()?.variable || '',
                identifier: 'mock-identifier-' + Date.now()
            };

            // Close dialog with the created content
            this.#dialogRef.close(mockCreatedContent);
        }, 3000);
    }
}
