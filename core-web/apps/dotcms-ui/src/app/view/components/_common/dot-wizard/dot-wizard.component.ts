import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    DestroyRef,
    inject,
    QueryList,
    signal,
    Type,
    ViewChild,
    ViewChildren
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { Dialog, DialogModule } from 'primeng/dialog';

import { filter, tap, delay } from 'rxjs/operators';

import { DotMessageService, DotWizardService } from '@dotcms/data-access';
import {
    DialogButton,
    DotDialogActions,
    DotWizardComponentEnum,
    DotWizardInput,
    DotWizardStep
} from '@dotcms/dotcms-models';

import { DotFormModel } from '../../../../shared/models/dot-form/dot-form.model';
import { DotContainerReferenceDirective } from '../../../directives/dot-container-reference/dot-container-reference.directive';
import { DotCommentAndAssignFormComponent } from '../forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '../forms/dot-push-publish-form/dot-push-publish-form.component';

@Component({
    selector: 'dot-wizard',
    templateUrl: './dot-wizard.component.html',
    styleUrls: ['./dot-wizard.component.scss'],
    imports: [CommonModule, DialogModule, ButtonModule, DotContainerReferenceDirective],
    providers: [DotWizardService]
})
export class DotWizardComponent implements AfterViewInit {
    #wizardData: { [key: string]: string };
    #currentStep = 0;
    #componentsHost: DotContainerReferenceDirective[];
    #stepsValidation: boolean[];
    #wizardComponentMap: { [key in DotWizardComponentEnum]: Type<unknown> } = {
        commentAndAssign: DotCommentAndAssignFormComponent,
        pushPublish: DotPushPublishFormComponent
    };

    readonly #componentFactoryResolver = inject(ComponentFactoryResolver);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotWizardService = inject(DotWizardService);
    readonly #destroyRef = inject(DestroyRef);

    readonly $data = signal<DotWizardInput>(null);
    readonly $dialogActions = signal<DotDialogActions | null>(null);
    readonly $stepsVisible = signal<boolean>(false);

    transform = '';

    @ViewChildren(DotContainerReferenceDirective)
    formHosts: QueryList<DotContainerReferenceDirective>;
    @ViewChild('dialog', { static: true }) dialog: Dialog;

    constructor() {
        this.#dotWizardService.showDialog$
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((data: DotWizardInput) => {
                this.$data.set(data);
            });
    }

    ngAfterViewInit(): void {
        // Listen for changes to formHosts QueryList
        this.formHosts.changes
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                filter(() => !!this.$data() && this.formHosts.length > 0),
                tap(() => {
                    this.loadComponents();
                    this.setDialogActions();
                }),
                delay(250), // needed for the components to be loaded and avoid FF focus issues.
                tap(() => {
                    this.$stepsVisible.set(true);
                }),
                delay(50) // needed for the first load after visible.
            )
            .subscribe(() => {
                this.focusFistFormElement();
            });
    }

    /**
     * Close the dialog and reset the wizard state
     * @memberof DotWizardComponent
     */
    close(): void {
        this.$data.set(null);
        this.#currentStep = 0;
        this.updateTransform();
        this.$stepsVisible.set(false);
    }

    /**
     * handle the tab event, so when is the last field of a a step
     * focus the next/submit button.
     * @param {KeyboardEvent} event
     * @memberof DotWizardComponent
     */
    handleTab(event: KeyboardEvent): void {
        const [form]: HTMLFieldSetElement[] = event
            .composedPath()
            .filter((x: Node) => x.nodeName === 'FORM') as HTMLFieldSetElement[];

        if (form) {
            if (form.elements.item(form.elements.length - 1) === event.target) {
                event.preventDefault();
                event.stopPropagation();
                const acceptButton = document.getElementsByClassName(
                    'dialog__button-accept'
                )[0] as HTMLButtonElement;
                acceptButton.focus();
            }
        }
    }

    getWizardComponent(type: DotWizardComponentEnum | string): Type<unknown> {
        return this.#wizardComponentMap[type];
    }

    private loadComponents(): void {
        this.#componentsHost = this.formHosts.toArray();
        this.#stepsValidation = [];
        this.$data().steps.forEach((step: DotWizardStep, index: number) => {
            const componentClass = this.getWizardComponent(step.component);
            const componentInstance =
                this.#componentFactoryResolver.resolveComponentFactory(componentClass);
            const viewContainerRef = this.#componentsHost[index].viewContainerRef;
            viewContainerRef.clear();
            const componentRef: ComponentRef<DotFormModel<unknown, unknown>> =
                viewContainerRef.createComponent(componentInstance) as ComponentRef<
                    DotFormModel<unknown, unknown>
                >;
            componentRef.instance.data = step.data;
            componentRef.instance.value
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((data: { [key: string]: string }) =>
                    this.consolidateValues(data, index)
                );
            componentRef.instance.valid
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((valid) => {
                    this.setValid(valid, index);
                });
        });
    }

    private consolidateValues(data: { [key: string]: string }, step: number): void {
        if (this.#stepsValidation[step] === true) {
            this.#wizardData = { ...this.#wizardData, ...data };
        }
    }

    private setDialogActions(): void {
        this.$dialogActions.set({
            accept: {
                action: () => {
                    this.getAcceptAction();
                },
                label: this.isLastStep()
                    ? this.#dotMessageService.get('send')
                    : this.#dotMessageService.get('next'),
                disabled: true
            },
            cancel: this.setCancelButton()
        });
    }

    private loadNextStep(next: number): void {
        this.#currentStep += next;
        this.updateTransform();
        this.focusFistFormElement();
        if (this.isLastStep()) {
            this.$dialogActions().accept.label = this.#dotMessageService.get('send');
            this.$dialogActions().cancel.disabled = false;
        } else if (this.isFirstStep()) {
            this.$dialogActions().cancel.disabled = true;
            this.$dialogActions().accept.label = this.#dotMessageService.get('next');
        } else {
            this.$dialogActions().cancel.disabled = false;
            this.$dialogActions().accept.label = this.#dotMessageService.get('next');
        }

        this.$dialogActions().accept.disabled = !this.#stepsValidation[this.#currentStep];
    }

    private getAcceptAction(): void {
        this.isLastStep() ? this.sendValue() : this.loadNextStep(1);
    }

    private isLastStep(): boolean {
        return this.#currentStep === this.#componentsHost.length - 1;
    }

    private isFirstStep(): boolean {
        return this.#currentStep === 0;
    }

    private setValid(valid: boolean, step: number): void {
        this.#stepsValidation[step] = valid;
        if (this.#currentStep === step) {
            this.$dialogActions().accept.disabled = !valid;
        }
    }

    private sendValue(): void {
        this.#dotWizardService.output$(this.#wizardData);
        this.close();
    }

    private updateTransform(): void {
        this.transform = `translateX(${this.#currentStep * 400 * -1}px)`;
    }

    private focusFistFormElement(): void {
        const stepContainer =
            this.#componentsHost[this.#currentStep]?.viewContainerRef?.element?.nativeElement
                ?.parentNode;

        if (stepContainer) {
            // Find all focusable elements and focus the first valid one
            const focusableElements = this.getFocusableElements(stepContainer);

            if (focusableElements.length > 0) {
                // Try to focus the first element
                this.attemptFocusElement(focusableElements[0]);
            }
        }
    }

    /**
     * Finds all focusable elements within a container
     * @param container The container to search within
     * @returns Array of focusable elements
     */
    private getFocusableElements(container: Element): HTMLElement[] {
        // Comprehensive selector for all potentially focusable elements
        const selector = `
            a[href]:not([tabindex='-1']),
            button:not([disabled]):not([tabindex='-1']),
            textarea:not([disabled]):not([tabindex='-1']),
            input:not([disabled]):not([tabindex='-1']),
            select:not([disabled]):not([tabindex='-1']),
            [tabindex]:not([tabindex='-1']),
            p-dropdown > .p-element,
            p-calendar > .p-element,
            p-inputmask > .p-element
        `;

        return Array.from(container.querySelectorAll(selector)) as HTMLElement[];
    }

    /**
     * Attempts to focus an element with special handling for PrimeNG components
     * @param element The element to focus
     */
    private attemptFocusElement(element: HTMLElement): void {
        // For PrimeNG components, find the actual input element
        if (element.classList.contains('p-element')) {
            // For dropdowns
            const dropdown = element.querySelector('input, button, .p-dropdown');
            if (dropdown) {
                (dropdown as HTMLElement).focus();

                return;
            }
        }

        // For standard elements
        element.focus();
    }

    private setCancelButton(): DialogButton {
        if (this.#componentsHost.length === 1) {
            return {
                action: () => this.close(),
                label: this.#dotMessageService.get('cancel'),
                disabled: false
            };
        }

        return {
            action: () => this.loadNextStep(-1),
            label: this.#dotMessageService.get('previous'),
            disabled: true
        };
    }
}
