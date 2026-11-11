import {
    Directive,
    EnvironmentInjector,
    Injector,
    OnInit,
    ViewContainerRef,
    inject,
    input,
    inputBinding,
    isSignal,
    outputBinding
} from '@angular/core';

import {
    DotSidePanelEntry,
    DotSidePanelRef
} from '../../services/dot-side-panel/dot-side-panel.service';

/**
 * Creates a panel's content where it sits, inside the panel's drawer. The content is created with
 * the opener's injectors, element and environment, so it resolves dependencies as if it were in the
 * opener's template, and with the panel's {@link DotSidePanelRef}, so it can close the panel.
 */
@Directive({ selector: '[dotSidePanelContent]' })
export class DotSidePanelContentDirective implements OnInit {
    readonly #viewContainer = inject(ViewContainerRef);

    /** The panel to fill. Its content must have loaded. */
    readonly panel = input.required<DotSidePanelEntry>({ alias: 'dotSidePanelContent' });

    ngOnInit(): void {
        const { ref, config, component } = this.panel();
        const type = component();

        if (!type) {
            return;
        }

        const inputs = Object.entries(config.inputs ?? {}).map(([name, value]) =>
            inputBinding(name, isSignal(value) ? value : () => value)
        );
        const outputs = Object.entries(config.outputs ?? {}).map(([name, handler]) =>
            outputBinding(name, handler)
        );

        const created = this.#viewContainer.createComponent(type, {
            injector: Injector.create({
                providers: [{ provide: DotSidePanelRef, useValue: ref }],
                parent: config.injector
            }),
            // Angular would take the environment from where this outlet sits, the root of the admin,
            // and miss what the opener's route provides, such as the page editor's MessageService.
            environmentInjector: config.injector.get(EnvironmentInjector),
            bindings: [...inputs, ...outputs]
        });

        ref.attach(created.instance);
    }
}
