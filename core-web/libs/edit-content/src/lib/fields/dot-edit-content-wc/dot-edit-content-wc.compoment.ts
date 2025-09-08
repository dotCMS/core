import { ChangeDetectionStrategy, Component, ElementRef, inject, input, AfterViewInit, viewChild, forwardRef, computed } from "@angular/core";
import { ControlContainer, ControlValueAccessor, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from "@angular/forms";

import { ButtonModule } from "primeng/button";
import { DialogModule } from "primeng/dialog";
import { InputTextModule } from "primeng/inputtext";

import { DotCMSContentlet, DotCMSContentTypeField } from "@dotcms/dotcms-models";
import { DotIconModule } from "@dotcms/ui";

/**
 * Interface for the dot-template-selector web component attributes
 */
interface DotWCElement extends HTMLElement {
    context?: Record<string, unknown>;
    form?: any;
    variableId?: string;
}

type DotWcEvent = {
    value: string | Record<string, unknown>;
    variableId: string;
};

@Component({
    selector: 'dot-edit-content-wc',
    template: '<div #container></div>',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        },
    ],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentWcCompoment),
            multi: true
        }
    ],
    imports: [
        DotIconModule,
        ButtonModule,
        InputTextModule,
        DialogModule,
        ReactiveFormsModule
    ],
})
export class DotEditContentWcCompoment implements ControlValueAccessor, AfterViewInit {
    /**
     * The field to render.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * The content type to render the field for.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    $variableId = computed(() => this.$field().variable);

    /**
     * The control container to get the form.
     */
    #controlContainer = inject(ControlContainer);

    $container = viewChild.required<ElementRef>('container');

    #webComponentInstance: DotWCElement = null;

    ngAfterViewInit() {
        this.#loadAndCreateComponent();
        this.#controlContainer.valueChanges.subscribe((value) => {
            this.#updateComponentContext(value);
        });
    }

    #loadScript(url: string): Promise<void> {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = url;
            script.onload = () => resolve();
            script.onerror = reject;
            document.body.appendChild(script);
        });
    }

    async #loadAndCreateComponent() {
        try {
            await this.#loadScript('http://localhost:4173/assets/index-DgGtroc6.js');

            const componentTag = 'dot-youtube-search';
            this.#webComponentInstance = document.createElement(componentTag) as DotWCElement;

            this.#webComponentInstance.addEventListener('dotChangeValue', (event: CustomEvent) => {
                console.log(event);
                if (this.onChange) {
                    this.onChange(event.detail.value);
                    this.onTouched();
                }
            });

            this.#webComponentInstance.addEventListener('dotChangeValues', (event: CustomEvent) => {
                console.log(event);
                this.#controlContainer.control.patchValue(event.detail.value, { emitEvent: false });
            });
            this.#assignFormToComponent();

            this.$container().nativeElement.appendChild(this.#webComponentInstance);

        } catch (error) {
            console.error(error);
        }
    }

    #updateComponentContext(context: Record<string, unknown>) {
        if (!this.#webComponentInstance) return;
        this.#webComponentInstance.context = context;
        this.#webComponentInstance.form = (this.#controlContainer as any).form;
    }

    #assignFormToComponent() {
        console.log((this.#controlContainer as any).form);
        if (!this.#webComponentInstance) return;
        this.#webComponentInstance.context = this.form.value;
        this.#webComponentInstance.form = (this.#controlContainer as any).form;
        this.#webComponentInstance.variableId = this.$variableId();
    }

    get form() {
        return this.#controlContainer.value;
    }

    writeValue(_: string): void {
        return;
    }

    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control is marked as touched in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    /**
     * A callback function that is called when the value of the field changes.
     * It is used to update the value of the field in the parent component.
     */
    private onChange: ((value: string) => void) | null = null;

    /**
     * A callback function that is called when the field is touched.
     */
    private onTouched: (() => void) | null = null;

}
