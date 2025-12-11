import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    inject,
    input,
    viewChild,
    computed,
    NgZone,
    OnInit,
    signal,
    OnDestroy
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule, FormGroupDirective } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import { createFormBridge, FormBridge } from '@dotcms/edit-content-bridge';
import { WINDOW } from '@dotcms/utils';

@Component({
    selector: 'dot-native-field',
    template: '<div #container></div>',
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
        }
    ],
    imports: [ButtonModule, InputTextModule, DialogModule, ReactiveFormsModule]
})
export class NativeFieldComponent implements OnInit, OnDestroy {
    /**
     * The DOM sanitizer to sanitize the template code.
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
     * The template code of the field.
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
            zone: this.#zone
        });

        this.#window['DotCustomFieldApi'] = this.#formBridge;
        this.$isBridgeReady.set(true);
    }

    readonly mountComponent = signalMethod<boolean>((isBridgeReady) => {
        if (!isBridgeReady) {
            return;
        }

        const templateCode = this.$templateCode();

        if (!templateCode) {
            return;
        }

        const hostElement = this.$container().nativeElement;

        // 1. Clear the content of the container
        hostElement.innerHTML = '';

        // 2. Parse the template code as HTML
        const parser = new DOMParser();
        const doc = parser.parseFromString(templateCode as string, 'text/html');

        // 3. Get the scripts
        const scripts = Array.from(doc.querySelectorAll('script'));

        // 4. Remove the parsed scripts from the virtual tree to avoid duplication
        // (The scripts parsed by DOMParser do not execute, but occupy space in the DOM if inserted as is)
        scripts.forEach((script) => script.remove());

        // 5. Append the nodes to the host element
        const nodes = Array.from(doc.body.childNodes);
        nodes.forEach((node) => hostElement.appendChild(node));

        // 6. Recreate and inject all scripts to execute them
        scripts.forEach((parsedScript) => {
            const scriptElement = this.#window.document.createElement('script');

            // A. Copy attributes (src, type, async, defer, integrity, etc.)
            // This is crucial for external libraries like Google Maps or Analytics
            Array.from(parsedScript.attributes).forEach((attr) => {
                scriptElement.setAttribute(attr.name, attr.value);
            });

            // B. Copy inline content if exists
            if (parsedScript.textContent) {
                scriptElement.textContent = parsedScript.textContent;
            }

            // C. Inject to execute
            // Note: Scripts with 'src' will be loaded asynchronously by default when inserted.
            hostElement.appendChild(scriptElement);
        });
    });

    ngOnDestroy(): void {
        if (this.#formBridge) {
            this.#formBridge.destroy();
        }
    }
}
