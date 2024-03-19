import {
    Directive,
    EmbeddedViewRef,
    Input,
    OnChanges,
    SimpleChange,
    SimpleChanges,
    TemplateRef,
    ViewContainerRef
} from '@angular/core';

class DotStringTemplateOutletContext {
    $implicit: unknown = null;
}

/**
 * Structural directive to give the ability of using an input of a component
 * to accept a string or a TemplateRef<any>, only add a ng-container with the
 * structural directive (`*dotStringTemplateOutlet`) $and send the input to the directive.
 *
 * at the component
 * @example
 * `@Input() title: string | TemplateRef<any>`
 *
 * at the template
 * @example
 * `<ng-container *dotStringTemplateOutlet="title">{{ title }}</ng-container>`
 *
 * And `title` shows the `TemplateRef` or the `string` sent by the input
 **/

@Directive({
    standalone: true,
    selector: '[dotStringTemplateOutlet]'
})
export class DotStringTemplateOutletDirective implements OnChanges {
    @Input() dotStringTemplateOutlet: unknown | TemplateRef<unknown> = null;
    private embeddedViewRef: EmbeddedViewRef<unknown> | null = null;
    private context = new DotStringTemplateOutletContext();

    constructor(
        private templateRef: TemplateRef<unknown>,
        private viewContainer: ViewContainerRef
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        const { dotStringTemplateOutlet } = changes;

        if (dotStringTemplateOutlet) {
            this.context.$implicit = dotStringTemplateOutlet.currentValue;
        }

        if (this.shouldRecreateView(dotStringTemplateOutlet)) {
            this.recreateView();
        }
    }

    private recreateView(): void {
        this.viewContainer.clear();
        const isTemplateRef = this.dotStringTemplateOutlet instanceof TemplateRef;
        const templateRef = (
            isTemplateRef ? this.dotStringTemplateOutlet : this.templateRef
        ) as unknown;
        this.embeddedViewRef = this.viewContainer.createEmbeddedView(
            templateRef as TemplateRef<unknown>,
            isTemplateRef ? this.dotStringTemplateOutlet : this.context
        );
    }

    private shouldRecreateView = (dotStringTemplateOutlet: SimpleChange): boolean => {
        return !!dotStringTemplateOutlet.firstChange;
    };
}
