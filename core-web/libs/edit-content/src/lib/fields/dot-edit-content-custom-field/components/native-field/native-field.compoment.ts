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
    OnInit
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

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFormBridge, FormBridge } from '@dotcms/edit-content-bridge';

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
        }
    ],
    imports: [ButtonModule, InputTextModule, DialogModule, ReactiveFormsModule]
})
export class NativeFieldComponent implements AfterViewInit, OnInit {
    /**
     * The field to render.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * The content type to render the field for.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    $variableId = computed(() => this.$field().variable);

    #formBridge: FormBridge;

    /**
     * The zone to run the code in.
     */
    #zone = inject(NgZone);

    /**
     * The control container to get the form.
     */
    #controlContainer = inject(ControlContainer);

    $container = viewChild.required<ElementRef>('container');

    $context = computed(() => this.$field().values);

    #webComponentInstance: HTMLElement | null = null;

    ngOnInit() {
        this.initializeFormBridge();
    }

    async ngAfterViewInit() {
        await this.loadCode();
    }

    async loadCode() {
        const jsCode = this.$context();
        this.updateContent(jsCode);
    }

    private updateContent(htmlString: string): void {
        const hostElement = this.$container().nativeElement;

        // 1. Limpiamos el contenido anterior
        hostElement.innerHTML = '';

        if (!htmlString) {
            return;
        }

        // 2. Regex para encontrar todas las etiquetas <script>
        // g = global, m = multilínea, i = insensible a mayúsculas
        const scriptRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gim;

        let scriptContents = '';
        let match;

        // 3. Extraemos el contenido de TODOS los scripts y los concatenamos
        while ((match = scriptRegex.exec(htmlString))) {
            scriptContents += match[1] + '\n'; // Agregamos el contenido del script
        }

        // 4. Obtenemos el HTML "limpio" (sin las etiquetas <script>)
        const htmlPart = htmlString.replace(scriptRegex, '');

        // 5. Insertamos el HTML en el elemento
        // Esto SÍ renderizará el HTML, pero no ejecutará los scripts
        hostElement.innerHTML = htmlPart;

        // 6. Creamos una nueva etiqueta <script> y la ejecutamos
        // Esta es la única forma de que el navegador ejecute JS insertado dinámicamente
        if (scriptContents) {
            const scriptElement = document.createElement('script');
            scriptElement.textContent = scriptContents;
            scriptElement.type = 'module';
            hostElement.appendChild(scriptElement);
        }
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

    private initializeFormBridge(): void {
        const form = (this.#controlContainer as FormGroupDirective).form;

        this.#formBridge = createFormBridge({
            type: 'angular',
            form,
            zone: this.#zone
        });

        window['DotCustomFieldApi'] = this.#formBridge;
    }
}
