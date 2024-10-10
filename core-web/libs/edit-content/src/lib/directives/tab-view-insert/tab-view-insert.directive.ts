import {
    AfterViewInit,
    Directive,
    inject,
    input,
    Renderer2,
    TemplateRef,
    ViewContainerRef
} from '@angular/core';

import { TabView } from 'primeng/tabview';

@Directive({
    selector: '[dotTabViewAppend]',
    standalone: true
})
export class TabViewInsertDirective implements AfterViewInit {
    $prependTpl = input<TemplateRef<unknown> | null>(null, { alias: 'dotTabViewPrepend' });
    $appendTpl = input<TemplateRef<unknown>>(null, { alias: 'dotTabViewAppend' });

    #viewContainer = inject(ViewContainerRef);
    #renderer = inject(Renderer2);
    #tabView = inject(TabView, { optional: true });

    ngAfterViewInit() {
        if (!this.#tabView) {
            console.warn('TabViewAppendDirective is for use with PrimeNG TabView');

            return;
        }

        this.insertContent();
    }

    private insertContent() {
        const tabViewElement = this.#tabView.el.nativeElement;
        const tabViewNavContent = tabViewElement.querySelector('.p-tabview-nav-content');

        if (!tabViewNavContent) {
            console.warn('TabView nav content not found');

            return;
        }

        if (this.$prependTpl()) {
            this.insertTemplate(this.$prependTpl(), tabViewNavContent, true);
        }

        if (this.$appendTpl()) {
            this.insertTemplate(this.$appendTpl(), tabViewNavContent, false);
        }
    }

    private insertTemplate(
        template: TemplateRef<unknown>,
        tabViewNavContent: Element,
        isPrepend: boolean
    ) {
        const viewRef = this.#viewContainer.createEmbeddedView(template);
        viewRef.detectChanges();

        viewRef.rootNodes.forEach((node) => {
            if (isPrepend) {
                this.#renderer.insertBefore(tabViewNavContent, node, tabViewNavContent.firstChild);
            } else {
                this.#renderer.appendChild(tabViewNavContent, node);
            }
        });

        if (viewRef.rootNodes.length > 0) {
            this.#renderer.addClass(
                viewRef.rootNodes[0],
                isPrepend ? 'tabview-prepend-content' : 'tabview-append-content'
            );
        }
    }
}
