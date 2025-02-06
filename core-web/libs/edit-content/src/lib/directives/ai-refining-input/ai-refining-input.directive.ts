import {
    ComponentRef,
    Directive,
    ElementRef,
    inject,
    input,
    OnInit,
    ViewContainerRef
} from '@angular/core';

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

    ngOnInit() {
        const inputElement = this.#elementRef.nativeElement as HTMLInputElement;
        this.menuComponent = this.#viewContainerRef.createComponent(DotAiMenuComponent);
        
        this.menuComponent.instance.textChanged.subscribe((text) => {
            inputElement.value = text;
        });

        inputElement.addEventListener('input', () => {
            const hasContent = inputElement.value.trim().length > 0;

            if (this.menuComponent) {
                this.menuComponent.setInput('text', inputElement.value);
            }

            const parent = inputElement.parentNode as HTMLElement;

            if (hasContent) {
                
                if (!parent.style.position) {
                    parent.style.position = 'relative';
                }
                
                const menuElement = this.menuComponent.location.nativeElement;
                parent.appendChild(menuElement);
            } else {
                parent.removeChild(this.menuComponent.location.nativeElement);
            }
        });
    }
}
