import { NgStyle } from '@angular/common';
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
import { createFormBridge, FormBridge } from '@dotcms/edit-content/bridge';
import { DotIconModule, SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

/**
 * This component is used to render a custom field in the DotCMS content editor.
 * It uses an iframe to render the custom field and provides a form bridge to communicate with the custom field.
 */
@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    imports: [SafeUrlPipe, NgStyle, DotIconModule, ButtonModule],
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
    ]
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

        if (!field || !contentType) {
            return '';
        }

        const params = new URLSearchParams({
            variable: contentType,
            field: field.variable
        });

        return `/html/legacy_custom_field/legacy-custom-field.jsp?${params}`;
    });

    /**
     * The title for the iframe.
     */
    $iframeTitle = computed(() => {
        const field = this.$field();

        return field ? `Content Type ${field.variable} and field ${field.name}` : '';
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
     * The cleanup function for the resize observer.
     */
    private resizeCleanup?: () => void;

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
     * Cleans up the custom field API and the resize observer.
     */
    ngOnDestroy(): void {
        this.#formBridge?.destroy();
        // Cleanup resize observer if it exists
        if (this.resizeCleanup) {
            this.resizeCleanup();
        }
    }

    /**
     * Adjusts the iframe height and sets up the resize observer.
     */
    ngAfterViewInit() {
        this.resizeCleanup = this.adjustIframeHeight();
    }

    /**
     * Adjusts the iframe height and sets up the resize observer.
     */
    private adjustIframeHeight() {
        const iframeEl = this.iframe()?.nativeElement;
        if (!iframeEl) {
            console.warn('Iframe not initialized');

            return () => {
                // nothing to do
            };
        }

        // Set initial height to 0 to prevent the 150px default
        iframeEl.style.height = '0px';

        const resizeObserver = new ResizeObserver(() => {
            try {
                const contentHeight = iframeEl.contentWindow?.document.documentElement.scrollHeight;
                if (contentHeight) {
                    iframeEl.style.height = `${contentHeight}px`;
                }
            } catch (error) {
                console.warn('Error adjusting iframe height:', error);
            }
        });

        const handleIframeLoad = () => {
            try {
                const body = iframeEl.contentWindow?.document.body;
                if (body) {
                    resizeObserver.observe(body);
                    const contentHeight =
                        iframeEl.contentWindow?.document.documentElement.scrollHeight;
                    if (contentHeight) {
                        iframeEl.style.height = `${contentHeight}px`;
                    }
                }
            } catch (error) {
                console.warn('Error setting up iframe resize observer:', error);
            }
        };

        iframeEl.addEventListener('load', handleIframeLoad);

        return () => {
            resizeObserver.disconnect();
            iframeEl.removeEventListener('load', handleIframeLoad);
        };
    }
}
