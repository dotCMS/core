import {
    ComponentRef,
    Directive,
    ElementRef,
    inject,
    input,
    OnInit,
    ViewContainerRef
} from '@angular/core';
import { NgControl } from '@angular/forms';

import { DotAiMenuComponent } from '../dot-ai/dot-ai-menu/dot-ai-menu.component';

@Directive({
    selector: '[dotAiRefiningInput]',
    standalone: true
})
export class AiRefiningInputDirective implements OnInit {
    private menuComponent!: ComponentRef<DotAiMenuComponent>;

    //** show the ai menu button */
    $showAiMenu = input<boolean>(false, { alias: 'showAiMenu' });

    #elementRef = inject(ElementRef);
    #viewContainerRef = inject(ViewContainerRef);
    #ngControl = inject(NgControl);

    ngOnInit() {
        const element = this.#elementRef.nativeElement as HTMLInputElement | HTMLTextAreaElement;
        this.menuComponent = this.#viewContainerRef.createComponent(DotAiMenuComponent);
        const parent = element.parentNode as HTMLElement;

        // Configurar el contenedor padre para el posicionamiento
        if (!parent.style.position) {
            parent.style.position = 'relative';
        }

        // Agregar el menú al DOM inmediatamente
        const menuElement = this.menuComponent.location.nativeElement;
        parent.appendChild(menuElement);

        // Observar cambios en el control
        this.#ngControl.control?.valueChanges.subscribe((value) => {
            console.log('value', value);
            const hasContent = value?.trim().length > 0;
            const isDirty = this.#ngControl.dirty;

            this.menuComponent.setInput('text', value || '');
            this.menuComponent.setInput('disabled', !hasContent || !isDirty);
            this.menuComponent.setInput('visible', isDirty);
        });

        this.menuComponent.instance.textChanged.subscribe((text) => {
            if (!text) return;
            this.#ngControl.control?.setValue(text);
            this.#ngControl.control?.markAsDirty();
        });
    }
}
