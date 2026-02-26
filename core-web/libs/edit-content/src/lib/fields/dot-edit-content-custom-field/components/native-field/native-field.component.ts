import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    input,
    NgZone,
    OnDestroy,
    OnInit,
    SecurityContext,
    signal,
    viewChild
} from '@angular/core';
import { ControlContainer, FormGroupDirective, ReactiveFormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFormBridge, FormBridge } from '@dotcms/edit-content-bridge';
import { WINDOW } from '@dotcms/utils';

/**
 * Renders custom fields using the native web components approach.
 * Supports both inline and modal display modes based on field configuration.
 * Provides a FormBridge API for bidirectional communication with custom field.
 */
@Component({
    selector: 'dot-native-field',
    template: '<div #container></div>',
    styleUrls: ['./native-field.component.scss'],
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
    imports: [ButtonModule, InputTextModule, DialogModule, ReactiveFormsModule]
})
export class NativeFieldComponent implements OnInit, OnDestroy {
    /**
     * The DOM sanitizer to validate the template code structure.
     * Note: We use sanitize() for validation only, not for blocking scripts/styles,
     * as those are required functionality for custom fields.
     */
    #domSanitizer = inject(DomSanitizer);
    /**
     * The field to render.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * The content type to render the field for.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * A readonly field that holds an instance of the DialogService.
     * This service is injected using Angular's dependency injection mechanism.
     * It is used to manage dialog interactions within the component.
     */
    readonly #dialogService = inject(DialogService);
    /**
     * The template code of the field.
     * This content is expected to be sanitized on the backend before reaching this component.
     */
    $templateCode = computed(() => {
        const rendered = this.$field().rendered;
        return rendered;
    });
    /**
     * The form bridge to communicate with the custom field.
     */
    #formBridge: FormBridge = null;
    /**
     * The zone to run the code in.
     */
    #zone = inject(NgZone);
    /**
     * The window object.
     */
    #window = inject(WINDOW);
    /**
     * The control container to get the form.
     */
    #controlContainer = inject(ControlContainer);
    /**
     * The container element to render the custom field in.
     */
    $container = viewChild.required<ElementRef>('container');
    /**
     * Whether the custom field is mounted.
     */
    $isBridgeReady = signal(false);
    /**
     * References to style elements created by this component.
     */
    #styleElements: HTMLStyleElement[] = [];

    constructor() {
        this.mountComponent(this.$isBridgeReady);
    }

    ngOnInit() {
        this.initializeFormBridge();
    }

    private initializeFormBridge(): void {
        const form = (this.#controlContainer as FormGroupDirective).form;

        this.#formBridge = createFormBridge({
            type: 'angular',
            form,
            zone: this.#zone,
            dialogService: this.#dialogService
        });

        this.#window['DotCustomFieldApi'] = this.#formBridge;
        this.$isBridgeReady.set(true);
    }

    /**
     * Validates that the template code has valid structure.
     * This performs basic validation - full sanitization must occur on the backend.
     *
     * SECURITY NOTE: This method does NOT sanitize content as scripts and styles are required.
     * All security validation and sanitization must be performed on the backend before
     * the content reaches this component.
     *
     * @param templateCode - The HTML template code to validate
     * @returns true if the template code passes basic validation
     */
    private validateTemplateCode(templateCode: string): boolean {
        if (!templateCode || typeof templateCode !== 'string') {
            return false;
        }

        // Basic validation: ensure it's not empty after trimming
        if (templateCode.trim().length === 0) {
            return false;
        }

        // Validate HTML structure using DOMParser to catch malformed HTML
        // Note: This only validates structure, not content security
        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(templateCode, 'text/html');
            const parserErrors = doc.querySelectorAll('parsererror');

            if (parserErrors.length > 0) {
                console.warn(
                    '[NativeFieldComponent] Malformed HTML detected in template code. ' +
                        'Content may not render correctly.'
                );
                // Still proceed - some parsing errors might be false positives
            }

            // Use DomSanitizer to check what would be sanitized (for informational purposes only)
            // This helps identify potential security concerns, but we don't block execution
            // Note: DomSanitizer will remove scripts/styles, which is expected behavior
            // but we need them for custom field functionality, so we proceed regardless
            try {
                this.#domSanitizer.sanitize(SecurityContext.HTML, templateCode);
                // Sanitization check completed - scripts/styles would be removed by sanitizer
                // but we proceed as they are required for custom field functionality
            } catch (error) {
                console.error('[NativeFieldComponent] Sanitization error:', error);
                return false;
            }

            return true;
        } catch (error) {
            console.error('[NativeFieldComponent] Template code validation error:', error);
            return false;
        }
    }

    readonly mountComponent = signalMethod<boolean>((isBridgeReady) => {
        if (!isBridgeReady) {
            return;
        }

        const templateCode = this.$templateCode();

        if (!templateCode) {
            return;
        }

        // Validate template code structure
        // SECURITY: This performs basic validation. Full sanitization must occur on the backend.
        if (!this.validateTemplateCode(templateCode)) {
            console.error('[NativeFieldComponent] Invalid template code structure');
            return;
        }

        const hostElement = this.$container().nativeElement;

        // If the container is already mounted, do nothing
        if (hostElement.innerHTML.length > 0) {
            return;
        }

        // 1. Clean up previous style elements
        this.#styleElements.forEach((styleElement) => {
            if (styleElement.parentNode) {
                styleElement.parentNode.removeChild(styleElement);
            }
        });
        this.#styleElements = [];

        // 2. Clear the content of the container
        hostElement.innerHTML = '';

        // 3. Parse the template code as HTML
        // SECURITY NOTE: This parses and executes arbitrary HTML/JS/CSS.
        // The content is assumed to be sanitized on the backend before reaching this component.
        const parser = new DOMParser();
        const doc = parser.parseFromString(templateCode, 'text/html');

        // Check for parsing errors (DOMParser may add error elements for malformed XML)
        const parserErrors = doc.querySelectorAll('parsererror');
        if (parserErrors.length > 0) {
            console.warn(
                '[NativeFieldComponent] HTML parsing warnings detected. Content may be malformed.'
            );
        }

        // 4. Get the scripts and styles
        const scripts = Array.from(doc.querySelectorAll('script'));
        const styles = Array.from(doc.querySelectorAll('style'));

        // 5. Remove the parsed scripts and styles from the virtual tree to avoid duplication
        // (The scripts parsed by DOMParser do not execute, but occupy space in the DOM if inserted as is)
        scripts.forEach((script) => script.remove());
        styles.forEach((style) => style.remove());

        // 6. Append the nodes to the host element
        const nodes = Array.from(doc.body.childNodes);
        nodes.forEach((node) => hostElement.appendChild(node));

        // 7. Insert styles into the document head
        // SECURITY: Styles are inserted as-is. Content must be sanitized on the backend.
        styles.forEach((parsedStyle) => {
            const styleElement = this.#window.document.createElement('style');

            // Copy attributes (type, media, etc.)
            // SECURITY: All attributes are copied. Backend should validate allowed attributes.
            Array.from(parsedStyle.attributes).forEach((attr) => {
                styleElement.setAttribute(attr.name, attr.value);
            });

            // Copy inline content
            if (parsedStyle.textContent) {
                styleElement.textContent = parsedStyle.textContent;
            }

            // Insert into document head
            this.#window.document.head.appendChild(styleElement);
            // Keep reference for cleanup
            this.#styleElements.push(styleElement);
        });

        // 8. Recreate and inject all scripts to execute them
        // SECURITY: Scripts are executed as-is. Content must be sanitized on the backend.
        // Consider implementing Content Security Policy (CSP) at the application level
        // to restrict script sources and execution contexts.
        scripts.forEach((parsedScript) => {
            const scriptElement = this.#window.document.createElement('script');

            // A. Copy attributes (src, type, async, defer, integrity, etc.)
            // This is crucial for external libraries like Google Maps or Analytics
            // SECURITY: All attributes are copied. Backend should validate:
            // - 'src' URLs are from trusted domains
            // - 'integrity' hashes match expected values
            // - 'type' is allowed (e.g., 'text/javascript', 'module')
            Array.from(parsedScript.attributes).forEach((attr) => {
                scriptElement.setAttribute(attr.name, attr.value);
            });

            // B. Copy inline content if exists
            // SECURITY: Inline scripts are executed. Backend must sanitize script content.
            if (parsedScript.textContent) {
                scriptElement.textContent = parsedScript.textContent;
            }

            // C. Inject to execute
            // Note: Scripts with 'src' will be loaded asynchronously by default when inserted.
            hostElement.appendChild(scriptElement);
        });
    });

    ngOnDestroy(): void {
        // Clean up style elements
        this.#styleElements.forEach((styleElement) => {
            if (styleElement.parentNode) {
                styleElement.parentNode.removeChild(styleElement);
            }
        });
        this.#styleElements = [];

        // Clean up hostElement completely
        const hostElement = this.$container()?.nativeElement;
        if (hostElement) {
            // Remove all child nodes
            while (hostElement.firstChild) {
                hostElement.removeChild(hostElement.firstChild);
            }
            // Clear innerHTML to ensure everything is removed
            hostElement.innerHTML = '';
        }

        if (this.#formBridge) {
            this.#formBridge.destroy();
        }
    }
}
