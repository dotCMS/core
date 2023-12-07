import { describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { Calendar } from 'primeng/calendar';

import { DotEditContentCalendarFieldComponent } from './dot-edit-content-calendar-field.component';
import { CALENDAR_OPTIONS_PER_TYPE } from './utils';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { DATE_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentCalendarFieldComponent', () => {
    let spectator: Spectator<DotEditContentCalendarFieldComponent>;
    let calendar: Element;

    const createComponent = createComponentFactory({
        component: DotEditContentCalendarFieldComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [FormGroupDirective]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: DATE_FIELD_MOCK
            }
        });
        calendar = spectator.query(byTestId(DATE_FIELD_MOCK.variable));
    });
    // ASK: This is necessary?
    test.each([
        {
            variable: `calendar-id-${DATE_FIELD_MOCK.variable}`,
            attribute: 'id'
        },
        {
            variable: DATE_FIELD_MOCK.variable,
            attribute: 'ng-reflect-name'
        }
    ])('should have the $variable as $attribute', ({ variable, attribute }) => {
        expect(calendar.getAttribute(attribute)).toBe(variable);
    });

    describe.each([
        {
            fieldType: FIELD_TYPES.DATE_AND_TIME
        },
        {
            fieldType: FIELD_TYPES.DATE
        },
        {
            fieldType: FIELD_TYPES.TIME
        }
    ])('with fieldType as $fieldType', ({ fieldType }) => {
        let calendar: Calendar;

        const options = CALENDAR_OPTIONS_PER_TYPE[fieldType];

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    field: { ...DATE_FIELD_MOCK, fieldType }
                }
            });

            calendar = spectator.debugElement.query(
                By.css(`[data-testId="${DATE_FIELD_MOCK.variable}"]`)
            ).componentInstance;
        });

        it('should have showTime as defined in the options', () => {
            expect(calendar.showTime).toBe(options.showTime);
        });

        it('should have the timeOnly as defined in the options', () => {
            expect(calendar.timeOnly).toBe(options.timeOnly);
        });

        it('should have the icon as defined in the options', () => {
            expect(calendar.icon).toBe(options.icon);
        });
    });
});
