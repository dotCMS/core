import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    HostListener,
    inject,
    input,
    NgZone,
    OnDestroy,
    signal,
    viewChild
} from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFormBridge, FormBridge } from '@dotcms/edit-content-bridge';
import { DotIconModule, SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

/**
 * This component is used to render a custom field in the DotCMS content editor.
 * It uses an iframe to render the custom field and provides a form bridge to communicate with the custom field.
 */
@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    imports: [SafeUrlPipe, DotIconModule, ButtonModule],
    templateUrl: './dot-edit-content-custom-field.component.html',
    styleUrls: ['./dot-edit-content-custom-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    host: {
        '[class.no-label]': '!$showLabel()'
    }
})
export class DotEditContentCustomFieldComponent implements OnDestroy, AfterViewInit {
    /**
     * The field to render.
     */
    $field = input<DotCMSContentTypeField>(null, { alias: 'field' });
    /**
     * The content type to render the field for.
     */
    $contentType = input<string>(null, { alias: 'contentType' });
    /**
     * The inode of the content to render the field for.
     */
    $inode = input<string>(null, { alias: 'inode' });
    /**
     * The iframe element to render the custom field in.
     */
    iframe = viewChild<ElementRef<HTMLIFrameElement>>('iframe');

    /**
     * The window object.
     */
    #window = inject(WINDOW);
    /**
     * The allowed origins to receive messages from.
     */
    private readonly ALLOWED_ORIGINS = [this.#window.location.origin];

    /**
     * Whether the iframe is in fullscreen mode.
     */
    $isFullscreen = signal(false);
    /**
     * The variables to pass to the custom field.
     */
    $variables = signal<Record<string, string>>({});

    /**
     * The source URL for the custom field.
     */
    $src = computed(() => {
        const field = this.$field();
        const contentType = this.$contentType();
        const inode = this.$inode() || '';

        if (!field || !contentType) {
            return '';
        }

        const params = new URLSearchParams({
            variable: contentType,
            field: field.variable,
            inode
        });

        return `/html/legacy_custom_field/legacy-custom-field.jsp?${params}`;
    });

    /**
     * Whether to show the label.
     */
    $showLabel = computed(() => {
        const field = this.$field();
        if (!field) return true;

        return field.fieldVariables.find(({ key }) => key === 'hideLabel')?.value !== 'true';
    });

    /**
     * The title for the iframe.
     */
    $iframeTitle = computed(() => {
        const field = this.$field();

        return field ? `Content Type ${field.variable} and field ${field.name}` : '';
    });

    /**
     * The minimum height for the container based on whether the label is shown or not.
     */
    $minContainerHeight = computed(() => {
        return this.$showLabel() ? '40px' : '17px';
    });

    /**
     * The form bridge to communicate with the custom field.
     */
    #formBridge: FormBridge;
    /**
     * The control container to get the form.
     */
    #controlContainer = inject(ControlContainer);
    /**
     * The zone to run the code in.
     */
    #zone = inject(NgZone);

    /**
     * The form to get the form.
     */
    $form = computed(() => (this.#controlContainer as FormGroupDirective).form);

    /**
     * Handles messages from the custom field and toggles fullscreen mode.
     * @param event - The message event.
     */
    @HostListener('window:message', ['$event'])
    onMessageFromCustomField({ data, origin }: MessageEvent) {
        if (!this.ALLOWED_ORIGINS.includes(origin)) {
            console.warn('Message received from unauthorized origin:', origin);

            return;
        }

        switch (data.type) {
            case 'toggleFullscreen':
                this.$isFullscreen.update((value) => !value);
                break;
        }
    }

    /**
     * Handles the iframe load event.
     */
    onIframeLoad() {
        const iframeEl = this.iframe()?.nativeElement;
        if (!iframeEl) return;

        iframeEl.classList.add('loaded');
        this.initializeFormBridge();
        this.$variables.set(this.initializeVariables());

        const iframeWindow = this.getIframeWindow();
        if (!iframeWindow) return;

        this.#zone.run(() => {
            this.initializeCustomFieldApi(iframeWindow);
        });
    }

    /**
     * Initializes the variables for the custom field.
     * @returns The variables for the custom field.
     */
    private initializeVariables(): Record<string, string> {
        return this.$field().fieldVariables.reduce(
            (acc, { key, value }) => {
                acc[key] = value;

                return acc;
            },
            {} as Record<string, string>
        );
    }

    /**
     * Initializes the form bridge.
     */
    private initializeFormBridge(): void {
        const form = (this.#controlContainer as FormGroupDirective).form;

        this.#formBridge = createFormBridge({
            type: 'angular',
            form,
            zone: this.#zone
        });
    }

    /**
     * Gets the iframe window.
     */
    private getIframeWindow(): Window | null {
        const iframeEl = this.iframe()?.nativeElement;
        if (!iframeEl) {
            console.warn('Iframe not initialized');

            return null;
        }

        const iframeWindow = iframeEl.contentWindow;
        if (!iframeWindow) {
            console.warn('Iframe window not available');

            return null;
        }

        return iframeWindow;
    }

    /**
     * Initializes the custom field API.
     * @param iframeWindow - The iframe window.
     */
    private initializeCustomFieldApi(iframeWindow: Window): void {
        try {
            if (!this.#formBridge) throw new Error('Form bridge not initialized');

            // Assign API only to iframe
            iframeWindow['DotCustomFieldApi'] = this.#formBridge;

            // Notify that the API is ready
            iframeWindow.postMessage({ type: 'dotcms:form:loaded' }, this.#window.location.origin);
        } catch (error) {
            console.error('Error initializing DotCustomFieldApi:', error);
        }
    }

    /**
     * Adjusts the iframe height and sets up the resize observer.
     */
    private adjustIframeHeight() {
        const iframeEl = this.iframe()?.nativeElement;
        if (!iframeEl) {
            return () => void 0;
        }

        // Set iframe styles to hide scrollbars
        iframeEl.style.overflow = 'hidden';
        iframeEl.style.scrollbarWidth = 'none'; // Firefox

        const updateHeight = () => {
            try {
                const body = iframeEl.contentWindow?.document.body;
                if (body) {
                    body.style.margin = '0';

                    // Add styles to body to hide scrollbars
                    body.style.overflow = 'hidden';
                    body.style.scrollbarWidth = 'none'; // Firefox

                    // Get scroll height and add a small buffer to prevent internal scrolling
                    // Without causing continuous growth
                    const height = body.scrollHeight;
                    if (height > 0) {
                        // Add a small buffer (2px) but use a data attribute to prevent continuous growth
                        const currentHeight = parseInt(iframeEl.dataset.lastHeight || '0', 10);

                        // Only update if height has changed or initial setting
                        if (!iframeEl.dataset.lastHeight || Math.abs(height - currentHeight) >= 2) {
                            const newHeight = height + 2; // Small buffer to avoid scrollbar
                            iframeEl.style.height = `${newHeight}px`;
                            iframeEl.dataset.lastHeight = newHeight.toString();
                        }
                    }
                }
            } catch (error) {
                console.warn('Error adjusting iframe height:', error);
            }
        };

        iframeEl.addEventListener('load', updateHeight);

        const observer = new MutationObserver(() => {
            requestAnimationFrame(updateHeight);
        });

        iframeEl.addEventListener('load', () => {
            const body = iframeEl.contentWindow?.document.body;
            if (body) {
                observer.observe(body, {
                    childList: true,
                    subtree: true,
                    attributes: true
                });
            }
        });

        return () => {
            observer.disconnect();
            iframeEl.removeEventListener('load', updateHeight);
        };
    }

    ngOnDestroy(): void {
        if (this.#formBridge) {
            this.#formBridge.destroy();
        }
    }

    /**
     * Adjusts the iframe height and sets up the resize observer.
     */
    ngAfterViewInit() {
        this.adjustIframeHeight();
    }
}
