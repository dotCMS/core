import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormArray, FormControl, FormGroup, FormGroupDirective, Validators } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import {
    DefaultGoalConfiguration,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals
} from '@dotcms/dotcms-models';
import { DotFieldValidationMessageComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsGoalConfigurationUrlParameterComponentComponent } from './dot-experiments-goal-configuration-url-parameter-component.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'Scheduling'
});

const formMock = new FormGroup({
    primary: new FormGroup({
        name: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required]
        }),
        type: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
        conditions: new FormArray([])
    })
});

const formGroupDirectiveMock = new FormGroupDirective([], []);
formGroupDirectiveMock.form = formMock;

describe('DotExperimentsGoalConfigurationUrlParameterComponentComponent', () => {
    let spectator: Spectator<DotExperimentsGoalConfigurationUrlParameterComponentComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsGoalConfigurationUrlParameterComponentComponent,
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
        spectator = createComponent({ detectChanges: false });
    });

    it('should be a VALID form if the inputs all filled', () => {
        const formValues: Goals = {
            primary: {
                ...DefaultGoalConfiguration.primary,
                name: 'default',
                type: GOAL_TYPES.URL_PARAMETER,
                conditions: [
                    {
                        parameter: GOAL_PARAMETERS.QUERY_PARAM,
                        operator: GOAL_OPERATORS.EQUALS,
                        value: {
                            name: 'test',
                            value: 'value'
                        }
                    }
                ]
            }
        };

        spectator.detectChanges();

        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();

        expect(spectator.component.form.valid).toBe(true);
    });

    it('should show DotFieldValidationMessageComponent message when the control is invalid', () => {
        const formValues: Goals = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.URL_PARAMETER,
                conditions: [
                    {
                        parameter: GOAL_PARAMETERS.QUERY_PARAM,
                        operator: GOAL_OPERATORS.EQUALS,
                        value: {
                            name: '',
                            value: ''
                        }
                    }
                ]
            }
        };

        spectator.detectChanges();

        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();

        expect(spectator.component.form.valid).toBe(false);
        expect(spectator.query(DotFieldValidationMessageComponent)).toExist();
    });

    it('should form be valid if `Operator` is `EXIST` and `value` is empty', () => {
        const formValues: Goals = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.URL_PARAMETER,
                conditions: [
                    {
                        parameter: GOAL_PARAMETERS.QUERY_PARAM,
                        operator: GOAL_OPERATORS.EXISTS,
                        value: {
                            name: 'queryParam-name',
                            value: ''
                        }
                    }
                ]
            }
        };

        spectator.detectChanges();

        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();

        expect(spectator.component.form.valid).toBe(true);
        expect(spectator.query(DotFieldValidationMessageComponent)).toExist();
    });

    it('should form be invalid if `Operator` is different to `EXIST`', () => {
        const formValues: Goals = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.URL_PARAMETER,
                conditions: [
                    {
                        parameter: GOAL_PARAMETERS.QUERY_PARAM,
                        operator: GOAL_OPERATORS.CONTAINS,
                        value: {
                            name: 'queryParam-name',
                            value: ''
                        }
                    }
                ]
            }
        };

        spectator.detectChanges();

        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();

        expect(spectator.component.form.valid).toBe(false);
        expect(spectator.query(DotFieldValidationMessageComponent)).toExist();
    });

    it('should show render OPERATOR Input, PARAMETER input and VALUE form inputs', () => {
        spectator.detectChanges();
        expect(spectator.query(byTestId('name-input'))).toExist();
        expect(spectator.query(byTestId('name-input'))).toBeInstanceOf(HTMLInputElement);
        expect(spectator.query(byTestId('operator-input'))).toExist();
        expect(spectator.query(byTestId('value-input'))).toExist();
    });
});
