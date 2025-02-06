import { ComponentRef, Directive, ElementRef, OnInit, ViewContainerRef } from '@angular/core';
import { DotAiMenuComponent } from '../dot-ai/dot-ai-menu/dot-ai-menu.component';

@Directive({
    selector: '[dotAiRefiningInput]',
    standalone: true
})
export class AiRefiningInputDirective implements OnInit {
    private menuComponent!: ComponentRef<DotAiMenuComponent>;

    constructor(
        private el: ElementRef,
        private viewContainerRef: ViewContainerRef
    ) {}

    ngOnInit() {
        const parent = this.el.nativeElement.parentNode;
        if (!parent.style.position) {
            parent.style.position = 'relative';
        }

        this.menuComponent = this.viewContainerRef.createComponent(DotAiMenuComponent);
        const menuElement = this.menuComponent.location.nativeElement;

        this.el.nativeElement.style.paddingRight = '2.357rem';

        parent.appendChild(menuElement);
    }
}
