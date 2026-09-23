import {
    AfterViewInit,
    Directive,
    inject,
    input,
    Renderer2,
    TemplateRef,
    ViewContainerRef
} from '@angular/core';

import { Tabs } from 'primeng/tabs';
/**
 * Directive to insert content into the tab view.
 * @deprecated Use the new Tabs API instead.
 */
@Directive({
    selector: '[dotTabViewAppend]'
})
export class TabViewInsertDirective implements AfterViewInit {
    $prependTpl = input<TemplateRef<unknown> | null>(null, { alias: 'dotTabViewPrepend' });
    $appendTpl = input<TemplateRef<unknown> | null>(null, { alias: 'dotTabViewAppend' });
    $prependContext = input<Record<string, unknown> | null>(null, {
        alias: 'dotTabViewPrependContext'
    });
    $appendContext = input<Record<string, unknown> | null>(null, {
        alias: 'dotTabViewAppendContext'
    });

    #viewContainer = inject(ViewContainerRef);
    #renderer = inject(Renderer2);
    #tabView = inject(Tabs, { optional: true });

    ngAfterViewInit() {
        if (!this.#tabView) {
            console.warn('TabViewAppendDirective is for use with PrimeNG Tabs');

            return;
        }

        this.insertContent(this.#tabView);
    }

    // Takes the narrowed instance as a parameter: `#tabView` is an optional injection, and the
    // null check in `ngAfterViewInit` does not narrow the field across a method boundary.
    private insertContent(tabView: Tabs) {
        const tabViewElement = tabView.el.nativeElement;
        // Try new tabs API structure first (.p-tablist), fallback to old (.p-tabview-nav-content)
        const tabViewNavContent =
            tabViewElement.querySelector('.p-tablist') ||
            tabViewElement.querySelector('.p-tabview-nav-content');

        if (!tabViewNavContent) {
            console.warn('TabView nav content not found');

            return;
        }

        const prependTpl = this.$prependTpl();

        if (prependTpl) {
            this.insertTemplate(prependTpl, tabViewNavContent, true, this.$prependContext());
        }

        const appendTpl = this.$appendTpl();

        if (appendTpl) {
            this.insertTemplate(appendTpl, tabViewNavContent, false, this.$appendContext());
        }
    }

    private insertTemplate(
        template: TemplateRef<unknown>,
        tabViewNavContent: Element,
        isPrepend: boolean,
        context: Record<string, unknown> | null = null
    ) {
        const viewRef = this.#viewContainer.createEmbeddedView(template, context ?? undefined);
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
