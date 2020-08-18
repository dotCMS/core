import {
    Component,
    ComponentFactoryResolver,
    Input,
    OnInit,
    QueryList,
    ViewChild,
    ViewChildren
} from '@angular/core';
import { DotContainerReferenceDirective } from '@directives/dot-container-reference/dot-container-reference.directive';
import { DotDialogActions, DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ComponentRef } from '@angular/core/src/linker/component_factory';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { OnDestroy } from '@angular/core/src/metadata/lifecycle_hooks';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DotWizardInput } from '@models/dot-wizard-input/dot-wizard-input.model';

@Component({
    selector: 'dot-wizard',
    templateUrl: './dot-wizard.component.html',
    styleUrls: ['./dot-wizard.component.scss']
})
export class DotWizardComponent implements OnInit, OnDestroy {
    wizardData: { [key: string]: string };
    dialogActions: DotDialogActions;
    transform = '';

    @Input() data: DotWizardInput;
    @ViewChildren(DotContainerReferenceDirective)
    formHosts: QueryList<DotContainerReferenceDirective>;
    @ViewChild('dialog') dialog: DotDialogComponent;

    private currentStep = 0;
    private componentsHost: DotContainerReferenceDirective[];
    private stepsValidation: boolean[];
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private dotMessageService: DotMessageService,
        private dotWizardService: DotWizardService
    ) {}

    ngOnInit() {
        this.dotWizardService.showDialog$.pipe(takeUntil(this.destroy$)).subscribe(data => {
            this.data = data;
            // need to wait to render the dotContainerReference.
            setTimeout(() => {
                this.loadComponents();
                this.setDialogActions();
                this.focusFistFormElement();
            }, 0);
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
        this.dialog.visible = false;
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
                const acceptButton = document.getElementsByClassName(
                    'dialog__button-accept'
                )[0] as HTMLButtonElement;
                acceptButton.focus();
                event.preventDefault();
            }
        }
    }

    /**
     * handle the enter event, if the form is valid move to the next step
     * @memberof DotWizardComponent
     */
    handleEnter(event: KeyboardEvent): void {
        event.stopImmediatePropagation();
        if (this.stepsValidation[this.currentStep]) {
            this.dialog.acceptAction();
        }
    }

    private loadComponents(): void {
        this.componentsHost = this.formHosts.toArray();
        this.stepsValidation = [];
        this.data.steps.forEach((step: DotWizardStep<any>, index: number) => {
            const comp = this.componentFactoryResolver.resolveComponentFactory(step.component);
            const viewContainerRef = this.componentsHost[index].viewContainerRef;
            viewContainerRef.clear();
            const componentRef: ComponentRef<any> = viewContainerRef.createComponent(comp);
            componentRef.instance.data = step.data;
            componentRef.instance.value
                .pipe(takeUntil(this.destroy$))
                .subscribe(data => this.consolidateValues(data, index));
            componentRef.instance.valid.pipe(takeUntil(this.destroy$)).subscribe(valid => {
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
        this.dialogActions = {
            accept: {
                action: () => {
                    this.getAcceptAction();
                },
                label: this.isLastStep()
                    ? this.dotMessageService.get('send')
                    : this.dotMessageService.get('next'),
                disabled: true
            },
            cancel: {
                action: () => {
                    this.loadNextStep(-1);
                },
                label: this.dotMessageService.get('previous'),
                disabled: true
            }
        };
    }

    private loadNextStep(next: number): void {
        this.currentStep += next;
        this.focusFistFormElement();
        this.updateTransform();
        if (this.isLastStep()) {
            this.dialogActions.accept.label = this.dotMessageService.get('send');
            this.dialogActions.cancel.disabled = false;
        } else if (this.isFirstStep()) {
            this.dialogActions.cancel.disabled = true;
            this.dialogActions.accept.label = this.dotMessageService.get('next');
        } else {
            this.dialogActions.cancel.disabled = false;
            this.dialogActions.accept.label = this.dotMessageService.get('next');
        }
        this.dialogActions.accept.disabled = !this.stepsValidation[this.currentStep];
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
            this.dialogActions.accept.disabled = !valid;
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
            const form: HTMLFormElement = this.componentsHost[
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
}
