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

        inputElement.addEventListener('input', () => {
            const hasContent = inputElement.value.trim().length > 0;
            const parent = this.#elementRef.nativeElement.parentNode as HTMLElement;

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
