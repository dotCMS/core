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
    selector: '[dotTabViewAppend]'
})
export class TabViewInsertDirective implements AfterViewInit {
    $prependTpl = input<TemplateRef<unknown> | null>(null, { alias: 'dotTabViewPrepend' });
    $appendTpl = input<TemplateRef<unknown> | null>(null, { alias: 'dotTabViewAppend' });

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

        const wrapper = this.#renderer.createElement('div');
        const testId = isPrepend ? 'tabview-prepend-content' : 'tabview-append-content';
        this.#renderer.setAttribute(wrapper, 'data-testid', testId);
        this.#renderer.addClass(wrapper, testId);

        viewRef.rootNodes.forEach((node) => {
            this.#renderer.appendChild(wrapper, node);
        });

        if (isPrepend) {
            this.#renderer.insertBefore(tabViewNavContent, wrapper, tabViewNavContent.firstChild);
        } else {
            this.#renderer.appendChild(tabViewNavContent, wrapper);
        }
    }
}
