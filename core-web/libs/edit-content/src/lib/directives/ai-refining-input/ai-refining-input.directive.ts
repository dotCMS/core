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

<<<<<<< HEAD
        this.menuComponent = this.viewContainerRef.createComponent(DotAiMenuComponent);
        const value = this.el.nativeElement.value;

        console.log(value);

        this.menuComponent.setInput('text', value);
        const menuElement = this.menuComponent.location.nativeElement;
=======
        inputElement.addEventListener('input', () => {
            const hasContent = inputElement.value.trim().length > 0;
            const parent = this.#elementRef.nativeElement.parentNode as HTMLElement;
>>>>>>> b412c9908012530b97bd62108b6253f5a428b3bb

            if (hasContent && !this.menuComponent) {
                const parent = inputElement.parentNode as HTMLElement;
                if (!parent.style.position) {
                    parent.style.position = 'relative';
                }

                this.menuComponent = this.#viewContainerRef.createComponent(DotAiMenuComponent);
                const menuElement = this.menuComponent.location.nativeElement;
                parent.appendChild(menuElement);
            } else if (!hasContent && this.menuComponent) {
                this.menuComponent.destroy();
                this.menuComponent = null!;
            }
        });
    }
}
