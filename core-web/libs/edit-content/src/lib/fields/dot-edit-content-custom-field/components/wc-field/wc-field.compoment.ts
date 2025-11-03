import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    inject,
    input,
    AfterViewInit,
    viewChild,
    forwardRef,
    computed
} from '@angular/core';
import {
    ControlContainer,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';


@Component({
    selector: 'dot-wc-field',
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
            useExisting: forwardRef(() => DotWCCompoment),
            multi: true
        }
    ],
    imports: [DotIconModule, ButtonModule, InputTextModule, DialogModule, ReactiveFormsModule]
})
export class DotWCCompoment implements AfterViewInit {
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

    $context = computed(() => this.$field().values);

    #webComponentInstance: HTMLElement | null = null;

    async ngAfterViewInit() {
        await this.loadAndCreateComponent();
        this.#controlContainer.valueChanges.subscribe((value) => {
            this.#updateComponentContext(value);
        });
    }

    async loadAndCreateComponent() {
        const jsCode = this.$context();
        const componentTag = 'dot-url-slug';

        // Solo define el componente si no existe
        if (!customElements.get(componentTag)) {
            const script = document.createElement('script');
            script.type = 'text/javascript';
            script.textContent = jsCode;
            document.body.appendChild(script);

            try {
                await customElements.whenDefined(componentTag);
            } catch (error) {
                console.error('Error waiting for custom element:', error);
                throw new Error(`Failed to define custom element: ${componentTag}`);
            }
        }

        const component = customElements.get(componentTag);
        if (!component) {
            throw new Error(`Component ${componentTag} not found after registration`);
        }

        // Crear la instancia del Web Component
        this.#webComponentInstance = document.createElement(componentTag) as HTMLElement;
        this.$container().nativeElement.appendChild(this.#webComponentInstance);

        // Escuchar eventos del Web Component
        this.#webComponentInstance.addEventListener('dotChangeValues', (event: CustomEvent) => {
            this.#controlContainer.control.patchValue(event.detail.value, { emitEvent: false });
        });
    }

    #updateComponentContext(context: Record<string, unknown>) {
        if (!this.#webComponentInstance) return;
        this.#webComponentInstance.setAttribute('context', JSON.stringify(context));
    }

    get form() {
        return this.#controlContainer.value;
    }

}
