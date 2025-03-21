import { Subject } from 'rxjs';

import {
    ChangeDetectorRef,
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    Input,
    OnDestroy,
    QueryList,
    signal,
    Type,
    ViewChild,
    ViewChildren
} from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { DotContainerReferenceDirective } from '@directives/dot-container-reference/dot-container-reference.directive';
import { DotMessageService, DotWizardService } from '@dotcms/data-access';
import {
    DialogButton,
    DotDialogActions,
    DotWizardComponentEnum,
    DotWizardInput,
    DotWizardStep
} from '@dotcms/dotcms-models';
import { DotDialogComponent } from '@dotcms/ui';
import { DotFormModel } from '@models/dot-form/dot-form.model';

import { DotCommentAndAssignFormComponent } from '../forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '../forms/dot-push-publish-form/dot-push-publish-form.component';

@Component({
    selector: 'dot-wizard',
    templateUrl: './dot-wizard.component.html',
    styleUrls: ['./dot-wizard.component.scss']
})
export class DotWizardComponent implements OnDestroy {
    wizardData: { [key: string]: string };
    $dialogActions = signal<DotDialogActions | null>(null);
    transform = '';

    @Input() data: DotWizardInput;
    @ViewChildren(DotContainerReferenceDirective)
    formHosts: QueryList<DotContainerReferenceDirective>;
    @ViewChild('dialog', { static: true }) dialog: DotDialogComponent;

    /**
     * Flag to track if save operation is in progress
     */
    isSaving = false;

    private currentStep = 0;
    private componentsHost: DotContainerReferenceDirective[];
    private stepsValidation: boolean[];
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private wizardComponentMap: { [key in DotWizardComponentEnum]: Type<unknown> } = {
        commentAndAssign: DotCommentAndAssignFormComponent,
        pushPublish: DotPushPublishFormComponent
    };

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private dotMessageService: DotMessageService,
        private dotWizardService: DotWizardService,
        private cd: ChangeDetectorRef
    ) {
        this.dotWizardService.showDialog$.pipe(takeUntil(this.destroy$)).subscribe((data) => {
            this.data = data;

            // need to wait to render the dotContainerReference.
            this.cd.detectChanges();
            setTimeout(() => {
                this.loadComponents();
                this.setDialogActions();
                this.cd.detectChanges();
                this.focusFistFormElement();
            }, 1000);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close the dialog and reset the wizard state
     * @memberof DotWizardComponent
     */
    close(): void {
        this.data = null;
        this.currentStep = 0;
        this.updateTransform();
    }

    /**
     * Handles tab key navigation to properly focus buttons after form elements
     * @param event Keyboard event
     */
    handleTab(event: KeyboardEvent): void {
        // Get the current step container
        const stepContainer =
            this.componentsHost[this.currentStep].viewContainerRef.element.nativeElement.parentNode;

        // Get all focusable elements in the current step
        const focusableElements = this.getFocusableElements(stepContainer);

        // Get dialog action buttons
        const actionButtons = Array.from(
            document.querySelectorAll('.dot-wizard__footer button')
        ) as HTMLElement[];

        // If focusing on the last form element and tabbing forward, focus the action button
        if (focusableElements.length > 0) {
            const focusedElement = document.activeElement as HTMLElement;
            const lastFormElement = focusableElements[focusableElements.length - 1];

            if (lastFormElement === focusedElement && actionButtons.length > 0) {
                event.preventDefault();
                // Focus the first button (usually Accept/Next)
                this.attemptFocusElement(actionButtons[0]);
            }
        }
    }

    getWizardComponent(type: DotWizardComponentEnum | string): Type<unknown> {
        return this.wizardComponentMap[type];
    }

    private loadComponents(): void {
        this.componentsHost = this.formHosts.toArray();
        this.stepsValidation = [];
        this.data.steps.forEach((step: DotWizardStep, index: number) => {
            const componentClass = this.getWizardComponent(step.component);
            const componentInstance =
                this.componentFactoryResolver.resolveComponentFactory(componentClass);
            const viewContainerRef = this.componentsHost[index].viewContainerRef;
            viewContainerRef.clear();
            const componentRef: ComponentRef<DotFormModel<unknown, unknown>> =
                viewContainerRef.createComponent(componentInstance) as ComponentRef<
                    DotFormModel<unknown, unknown>
                >;
            componentRef.instance.data = step.data;
            componentRef.instance.value
                .pipe(takeUntil(this.destroy$))
                .subscribe((data: { [key: string]: string }) =>
                    this.consolidateValues(data, index)
                );
            componentRef.instance.valid.pipe(takeUntil(this.destroy$)).subscribe((valid) => {
                this.setValid(valid, index);
            });
        });
    }

    private consolidateValues(data: { [key: string]: string }, step: number): void {
        if (this.stepsValidation[step] === true) {
            this.wizardData = { ...this.wizardData, ...data };
        }
    }

    private setDialogActions(): void {
        this.$dialogActions.set({
            accept: {
                action: () => {
                    this.getAcceptAction();
                },
                label: this.isLastStep()
                    ? this.dotMessageService.get('send')
                    : this.dotMessageService.get('next'),
                disabled: true
            },
            cancel: this.setCancelButton()
        });
    }

    private loadNextStep(next: number): void {
        this.currentStep += next;
        this.focusFistFormElement();
        this.updateTransform();
        this.updateNavigationButtons();
    }

    private getAcceptAction(): void {
        this.isLastStep() ? this.sendValue() : this.loadNextStep(1);
    }

    private isLastStep(): boolean {
        return this.currentStep === this.componentsHost.length - 1;
    }

    private isFirstStep(): boolean {
        return this.currentStep === 0;
    }

    private setValid(valid: boolean, step: number): void {
        this.stepsValidation[step] = valid;
        if (this.currentStep === step) {
            this.$dialogActions().accept.disabled = !valid;
        }
    }

    private sendValue(): void {
        this.isSaving = true;
        this.dotWizardService.output$(this.wizardData);

        // Add a slight delay to show the loading state
        setTimeout(() => {
            this.isSaving = false;
            this.close();
        }, 300);
    }

    private updateTransform(): void {
        this.transform = `translateX(${this.currentStep * 400 * -1}px)`;
    }

    private focusFistFormElement(): void {
        // Wait for Angular change detection to complete
        setTimeout(() => {
            const stepContainer =
                this.componentsHost[this.currentStep].viewContainerRef.element.nativeElement
                    .parentNode;

            // Find all focusable elements and focus the first valid one
            const focusableElements = this.getFocusableElements(stepContainer);

            if (focusableElements.length > 0) {
                // Try to focus the first element
                this.attemptFocusElement(focusableElements[0]);
            }
        }, 100);
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
        if (this.componentsHost.length === 1) {
            return {
                action: () => this.close(),
                label: this.dotMessageService.get('cancel'),
                disabled: false
            };
        }

        return {
            action: () => this.loadNextStep(-1),
            label: this.dotMessageService.get('previous'),
            disabled: true
        };
    }

    /**
     * Updates the navigation buttons based on current step
     */
    private updateNavigationButtons(): void {
        if (!this.$dialogActions()) {
            return;
        }

        // Update accept button
        this.$dialogActions().accept.label = this.isLastStep()
            ? this.dotMessageService.get('send')
            : this.dotMessageService.get('next');
        this.$dialogActions().accept.disabled = !this.stepsValidation[this.currentStep];

        // Update cancel/previous button
        this.$dialogActions().cancel.disabled = this.isFirstStep();
        this.$dialogActions().cancel.label =
            this.componentsHost.length > 1 && !this.isFirstStep()
                ? this.dotMessageService.get('previous')
                : this.dotMessageService.get('cancel');
    }
}
