import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
    DotExperiment,
    DotStoreWithSidebar
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { SidebarModule } from 'primeng/sidebar';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { InputTextModule } from 'primeng/inputtext';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { Observable, throwError } from 'rxjs';
import { switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MessageService } from 'primeng/api';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';

type DotExperimentAddVariant = DotStoreWithSidebar;

interface AddForm {
    name: FormControl<string>;
}

@Component({
    selector: 'dot-experiments-configuration-variants-add',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotSidebarHeaderComponent,
        DotMessagePipeModule,
        DotFieldValidationMessageModule,
        DotSidebarDirective,
        //PrimeNg
        SidebarModule,
        ButtonModule,
        InputTextModule
    ],
    templateUrl: './dot-experiments-configuration-variants-add.component.html',
    styleUrls: ['./dot-experiments-configuration-variants-add.component.scss'],
    providers: [ComponentStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsAddComponent implements OnInit {
    /**
     * Emit when the sidebar is closed
     */
    @Output()
    closedSidebar = new EventEmitter<void>();

    vm$ = this.componentStore.state$;
    form: FormGroup<AddForm>;

    private readonly addVariant = this.componentStore.effect(
        (variant$: Observable<Pick<DotExperiment, 'name'>>) => {
            return variant$.pipe(
                tap(() => this.componentStore.patchState({ isSaving: true })),
                withLatestFrom(this.dotExperimentsConfigurationStore.getExperimentId$),
                switchMap(([variant, experimentId]) =>
                    this.dotExperimentsService.addVariant(experimentId, variant).pipe(
                        tapResponse(
                            (experiment) => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get(
                                        'experiments.configure.variant.add.confirm-title'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'experiments.configure.variant.add.confirm-message',
                                        experiment.name
                                    )
                                });

                                this.dotExperimentsConfigurationStore.setTrafficProportion(
                                    experiment.trafficProportion
                                );
                                this.closeSidebar();
                            },
                            (error: HttpErrorResponse) => throwError(error)
                        )
                    )
                )
            );
        }
    );

    constructor(
        private readonly componentStore: ComponentStore<DotExperimentAddVariant>,
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private readonly messageService: MessageService
    ) {
        this.componentStore.setState({
            isSaving: false,
            isOpenSidebar: false
        });
    }

    handleSubmit(): void {
        const formValues = this.form.value as Pick<DotExperiment, 'name'>;
        this.addVariant(formValues);
    }

    closeSidebar() {
        this.componentStore.patchState({ isOpenSidebar: false });
        this.closedSidebar.emit();
    }

    ngOnInit(): void {
        this.initForm();
        this.componentStore.patchState({ isOpenSidebar: true });
    }

    private initForm() {
        this.form = new FormGroup<AddForm>({
            name: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required, Validators.maxLength(255)]
            })
        });
    }
}
