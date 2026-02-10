import {
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
import { ControlContainer, FormGroupDirective, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFormBridge, FormBridge } from '@dotcms/edit-content-bridge';
import { SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

import { CustomFieldConfig } from '../../../../models/dot-edit-content-custom-field.interface';
import { DEFAULT_CUSTOM_FIELD_CONFIG } from '../../../../models/dot-edit-content-field.constant';
import { createCustomFieldConfig } from '../../../../utils/functions.util';
import { INPUT_TEXT_OPTIONS } from '../../../dot-edit-content-text-field/utils';

/**
 * Renders custom fields using the legacy iframe-based approach (VTL templates).
 * Supports both inline and modal display modes based on field configuration.
 * Provides a FormBridge API for bidirectional communication with iframe content.
 */
@Component({
    selector: 'dot-iframe-field',
    imports: [SafeUrlPipe, ButtonModule, InputTextModule, DialogModule, ReactiveFormsModule],
    templateUrl: './iframe-field.component.html',
    styleUrls: ['./iframe-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [
        {
            provide: WINDOW,
            useValue: window
        },
        DialogService
    ],
    host: {
        '[class.no-label]': '!$showLabel()'
    }
})
export class IframeFieldComponent implements OnDestroy {
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
    $iframe = viewChild<ElementRef<HTMLIFrameElement>>('iframe');
    /**
     * The contentlet to render the field for.
     */
    $contentlet = input<DotCMSContentlet>(null, { alias: 'contentlet' });
    /**
     * Whether to show the label.
     */
    $showLabel = input<boolean>(true, { alias: 'showLabel' });
    /**
     * The inode of the content to render the field for.
     */
    $inode = computed(() => this.$contentlet()?.inode);
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
        const fieldConfig = this.$fieldConfig();

        if (!field || !contentType) {
            return '';
        }

        const params = new URLSearchParams({
            variable: contentType,
            field: field.variable,
            inode
        });

        // Add modal parameter if in modal mode
        if (fieldConfig.showAsModal) {
            params.set('modal', 'true');
        }

        const url = `/html/legacy_custom_field/legacy-custom-field.jsp?${params}`;

        return url;
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
     * Computed field configuration that combines default values with field variables.
     */
    $fieldConfig = computed((): CustomFieldConfig => {
        const field = this.$field();

        if (!field?.fieldVariables) {
            // Return default config if no field variables
            return DEFAULT_CUSTOM_FIELD_CONFIG;
        }

        return createCustomFieldConfig(field.fieldVariables);
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
     * A private field that holds an instance of the DialogService.
     * This service is injected using Angular's dependency injection mechanism.
     * It is used to manage dialog interactions within the component.
     */
    readonly #dialogService = inject(DialogService);
    /**
     * The form to get the form.
     */
    $form = computed(() => (this.#controlContainer as FormGroupDirective).form);

    /**
     * Whether the modal dialog is visible
     */
    $showModal = signal(false);

    /**
     * The input text options for the custom field.
     */
    protected readonly inputTextOptions = INPUT_TEXT_OPTIONS;

    /**
     * Handles messages from the custom field and toggles fullscreen mode.
     * @param event - The message event.
     */
    @HostListener('window:message', ['$event'])
    onMessageFromCustomField({ data, origin }: MessageEvent) {
        if (!this.ALLOWED_ORIGINS.includes(origin)) {
            console.warn('❌ Message received from unauthorized origin:', origin);

            return;
        }

        switch (data.type) {
            case 'toggleFullscreen':
                this.$isFullscreen.update((value) => !value);
                break;

            case 'dotcms:iframe:resize':
                this.handleIframeResize(data.height, data.fieldVariable);
                break;

            default:
                console.warn('❓ Unknown message type:', data.type);
        }
    }

    /**
     * Handles iframe resize requests from the custom field.
     * @param height - The new height for the iframe.
     * @param fieldVariable - The field variable to identify which iframe sent the message.
     * @param iframeId - The unique iframe identifier.
     */
    private handleIframeResize(height: number, fieldVariable?: string): void {
        // Skip auto-resize for modal mode - use fixed dimensions from config
        const fieldConfig = this.$fieldConfig();
        if (fieldConfig.showAsModal) {
            return;
        }

        // Check if this message is for this specific iframe
        const currentFieldVariable = this.$field()?.variable;
        if (fieldVariable && currentFieldVariable !== fieldVariable) {
            return;
        }

        const iframeEl = this.$iframe()?.nativeElement;
        if (!iframeEl) {
            return;
        }

        if (!height || height <= 0) {
            return;
        }

        // Update iframe height smoothly
        iframeEl.style.height = `${height}px`;
        iframeEl.dataset.lastHeight = height.toString();

        // Ensure iframe allows dropdowns to be visible
        iframeEl.style.overflow = 'visible';
        iframeEl.style.zIndex = '1000';
    }

    /**
     * Handles the iframe load event.
     */
    onIframeLoad() {
        const iframeEl = this.$iframe()?.nativeElement;
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
            zone: this.#zone,
            dialogService: this.#dialogService
        });
    }

    /**
     * Gets the iframe window.
     */
    private getIframeWindow(): Window | null {
        const iframeEl = this.$iframe()?.nativeElement;
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
     * Opens the custom field in a modal dialog
     */
    openModal(): void {
        this.$showModal.set(true);
    }

    /**
     * Closes the modal dialog
     */
    closeModal(): void {
        this.$showModal.set(false);
    }

    ngOnDestroy(): void {
        if (this.#formBridge) {
            this.#formBridge.destroy();
        }
    }
}
