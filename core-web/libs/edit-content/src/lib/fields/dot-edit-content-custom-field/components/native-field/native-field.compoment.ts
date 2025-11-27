import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    inject,
    input,
    AfterViewInit,
    viewChild,
    forwardRef,
    computed,
    NgZone,
    OnInit,
    signal
} from '@angular/core';
import {
    ControlContainer,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule,
    FormGroupDirective
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import { createFormBridge, FormBridge } from '@dotcms/edit-content-bridge';
import { WINDOW } from '@dotcms/utils';
import { signalMethod } from '@ngrx/signals';

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
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => NativeFieldComponent),
            multi: true
        },
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    imports: [ButtonModule, InputTextModule, DialogModule, ReactiveFormsModule]
})
export class NativeFieldComponent implements OnInit {
    /**
     * The field to render.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * The content type to render the field for.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * The variable id of the field.
     */
    $variableId = computed(() => this.$field().variable);
    /**
     * The template code of the field.
     */
    $templateCode = computed(() => this.$field().values);
    /**
     * The form bridge to communicate with the custom field.
     */
    #formBridge: FormBridge;
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

    get form() {
        return this.#controlContainer.value;
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

        // 2. Regex to find all <script> tags
        const scriptRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gim;

        let scriptContents = '';
        let match;

        // 3. Extract the content of ALL scripts and concatenate them
        while ((match = scriptRegex.exec(templateCode))) {
            scriptContents += match[1] + '\n'; // Add the content of the script
        }

        // 4. Get the HTML "clean" (without the <script> tags)
        const htmlPart = templateCode.replace(scriptRegex, '');

        // 5. Insert the HTML into the element
        // This will render the HTML, but not execute the scripts
        hostElement.innerHTML = htmlPart;

        // 6. Create a new <script> tag and execute it
        // This is the only way for the browser to execute JS inserted dynamically
        if (scriptContents) {
            const scriptElement = document.createElement('script');
            scriptElement.textContent = scriptContents;
            scriptElement.type = 'module';
            hostElement.appendChild(scriptElement);
        }
    });
}
