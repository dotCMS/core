import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormArray, FormControl, FormGroup, FormGroupDirective, Validators } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotFieldValidationMessageComponent } from '@components/_common/dot-field-validation-message/dot-field-validation-message';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMessageService } from '@dotcms/data-access';
import {
    DefaultGoalConfiguration,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsGoalConfigurationReachPageComponent } from './dot-experiments-goal-configuration-reach-page.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.sidebar.header': 'Select a goal',
    'experiments.configure.goals.sidebar.header.button': 'Apply'
});

const formMock = new FormGroup({
    primary: new FormGroup({
        name: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required]
        }),
        type: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
        conditions: new FormArray([
            new FormGroup({
                parameter: new FormControl('', { validators: [Validators.required] }),
                operator: new FormControl('', { validators: [Validators.required] }),
                value: new FormControl('', { validators: [Validators.required] })
            })
        ])
    })
});
const formGroupDirectiveMock = new FormGroupDirective([], []);
formGroupDirectiveMock.form = formMock;

describe('DotExperimentsExperimentGoalReachPageConfigComponent', () => {
    let spectator: Spectator<DotExperimentsGoalConfigurationReachPageComponent>;

    const createComponent = createComponentFactory({
        imports: [DropdownModule, DotFieldValidationMessageModule],
        component: DotExperimentsGoalConfigurationReachPageComponent,

        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: FormGroupDirective,
                useValue: formGroupDirectiveMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent();
    });

    it('should be a VALID form if the inputs all filled', () => {
        const formValues: Goals = {
            primary: {
                ...DefaultGoalConfiguration.primary,
                name: 'default',
                type: GOAL_TYPES.BOUNCE_RATE
            }
        };

        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();

        expect(spectator.component.form.valid).toEqual(true);
    });

    it('should show DotFieldValidationMessageComponent message when the control is invalid', () => {
        const formValues: Goals = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.BOUNCE_RATE,
                conditions: [
                    {
                        parameter: GOAL_PARAMETERS.URL,
                        operator: GOAL_OPERATORS.EQUALS,
                        value: ''
                    }
                ]
            }
        };

        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();

        expect(spectator.component.form.valid).toEqual(false);
        expect(spectator.query(DotFieldValidationMessageComponent)).toExist();
    });

    it('should show render OPERATOR Input, PARAMETER input and VALUE form inputs', () => {
        expect(spectator.query(byTestId('parameter-input'))).toExist();
        expect(spectator.query(byTestId('operator-input'))).toExist();
        expect(spectator.query(byTestId('value-input'))).toExist();
    });
});
