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
        if (this.isLastStep()) {
            this.$dialogActions().accept.label = this.dotMessageService.get('send');
            this.$dialogActions().cancel.disabled = false;
        } else if (this.isFirstStep()) {
            this.$dialogActions().cancel.disabled = true;
            this.$dialogActions().accept.label = this.dotMessageService.get('next');
        } else {
            this.$dialogActions().cancel.disabled = false;
            this.$dialogActions().accept.label = this.dotMessageService.get('next');
        }

        this.$dialogActions().accept.disabled = !this.stepsValidation[this.currentStep];
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
        this.dotWizardService.output$(this.wizardData);
        this.close();
    }

    private updateTransform(): void {
        this.transform = `translateX(${this.currentStep * 400 * -1}px)`;
    }

    private focusFistFormElement(): void {
        let count = 0;
        // need to wait dynamic component to load the form.
        const interval = setInterval(() => {
            const form: HTMLFormElement =
                this.componentsHost[
                    this.currentStep
                ].viewContainerRef.element.nativeElement.parentNode.children[0].getElementsByTagName(
                    'form'
                )[0];
            if (form || count === 10) {
                clearInterval(interval);
                (form.elements[0] as HTMLElement).focus();
            }

            count++;
        }, 200);
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
}
