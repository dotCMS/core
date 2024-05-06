import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    BINARY_OPTION,
    DotBinaryOptionSelectorComponent
} from './dot-binary-option-selector.component';

const messageServiceMock = new MockDotMessageService({
    'dot.mock.option1.message': 'Message 1',
    'dot.mock.option2.message': 'Message 2',
    'dot.mock.option1.label': 'Option 1',
    'dot.mock.option2.label': 'Option 2',
    next: 'Next'
});

const DATA_MOCK: BINARY_OPTION = {
    option1: {
        value: 'option1',
        message: 'dot.mock.option1.message',
        icon: 'article',
        label: 'dot.mock.option1.label'
    },
    option2: {
        value: 'option2',
        message: 'dot.mock.option2.message',
        icon: 'dynamic_feed',
        label: 'dot.mock.option2.label'
    }
};

describe('DotBinaryOptionSelectorComponent', () => {
    let component: DotBinaryOptionSelectorComponent;
    let fixture: ComponentFixture<DotBinaryOptionSelectorComponent>;
    let de: DebugElement;

    /**
     * let dynamicDialogConfig: DynamicDialogConfig;
     */
    let dynamicDialogRef: DynamicDialogRef;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotBinaryOptionSelectorComponent, DotMessagePipe],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: () => {
                            /** */
                        }
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            options: DATA_MOCK
                        }
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBinaryOptionSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        /**
         * dynamicDialogConfig = TestBed.inject(DynamicDialogConfig);
         */
        dynamicDialogRef = TestBed.inject(DynamicDialogRef);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('Option 1', () => {
        let option1: DebugElement;

        beforeEach(() => {
            option1 = de.query(By.css('[data-testId="option1"]'));
        });

        it('should have radio button', () => {
            fixture.detectChanges();
            const radio = option1.query(By.css('input[type="radio"]'));

            const { type, id, name } = radio.attributes;
            expect({
                type,
                id,
                name
            }).toEqual({
                type: 'radio',
                id: 'option1',
                name: 'options'
            });

            expect(radio.attributes['ng-reflect-value']).toEqual(DATA_MOCK.option1.value);
        });

        it('should have icon', () => {
            const icon = option1.query(By.css('.material-icons'));

            expect(icon).toBeTruthy();
        });

        it('should have label', () => {
            const label = option1.query(By.css('[data-testId="label-op1"]'));
            expect(label.nativeElement.innerText).toBe(
                messageServiceMock.get(DATA_MOCK.option1.label)
            );
        });

        it('should change to first option', () => {
            const input = de.query(By.css(`[data-testId="option2"] input[type="radio"]`));
            input.triggerEventHandler('change', { target: { checked: true } });
            fixture.detectChanges();
            expect(component.value).toBe(DATA_MOCK.option2.value);

            const input2 = de.query(By.css(`[data-testId="option1"] input[type="radio"]`));
            input2.triggerEventHandler('change', { target: { checked: true } });
            fixture.detectChanges();
            expect(component.value).toBe(DATA_MOCK.option1.value);
        });
    });

    describe('Option 2', () => {
        let option2: DebugElement;

        beforeEach(() => {
            option2 = de.query(By.css('[data-testId="option2"]'));
        });

        it('should have radio button', () => {
            fixture.detectChanges();
            const radio = option2.query(By.css('input[type="radio"]'));

            const { type, id, name } = radio.attributes;
            expect({
                type,
                id,
                name
            }).toEqual({
                type: 'radio',
                id: 'option2',
                name: 'options'
            });

            expect(radio.attributes['ng-reflect-value']).toEqual(DATA_MOCK.option2.value);
        });

        it('should have icon', () => {
            const icon = option2.query(By.css('.material-icons'));

            expect(icon).toBeTruthy();
        });

        it('should have label', () => {
            const label = option2.query(By.css('[data-testId="label-op2"]'));
            expect(label.nativeElement.innerText).toBe(
                messageServiceMock.get(DATA_MOCK.option2.label)
            );
        });

        it('should change to second option', () => {
            const input = de.query(By.css(`[data-testId="option2"] input[type="radio"]`));
            input.triggerEventHandler('change', { target: { checked: true } });
            fixture.detectChanges();
            expect(component.value).toBe(DATA_MOCK.option2.value);
        });
    });

    describe('description', () => {
        it('should have innerText of option1 by default', () => {
            const description = de.query(By.css('[data-testId="description"]'));
            expect(description.nativeElement.innerText).toBe(
                messageServiceMock.get(DATA_MOCK.option1.message)
            );
        });

        it('should have innerText of option2 by default', () => {
            const description = de.query(By.css('[data-testId="description"]'));
            const option2 = de.query(By.css('[data-testId="option2"] input[type="radio"]'));

            option2.nativeElement.click();
            fixture.detectChanges();

            expect(description.nativeElement.innerText).toBe(
                messageServiceMock.get(DATA_MOCK.option2.message)
            );
        });
    });

    describe('button', () => {
        it('should close dialog and emit the current value', () => {
            const button = de.query(By.css('[data-testId="button"]'));
            const spy = spyOn(dynamicDialogRef, 'close');
            button.triggerEventHandler('click', null);
            expect(spy).toHaveBeenCalledWith(DATA_MOCK.option1.value);
        });

        it('should change selection, close dialog and emit the current value', () => {
            const input = de.query(By.css(`[data-testId="option2"] input[type="radio"]`));
            input.triggerEventHandler('change', { target: { checked: true } });
            fixture.detectChanges();

            const button = de.query(By.css('[data-testId="button"]'));
            const spy = spyOn(dynamicDialogRef, 'close');
            button.triggerEventHandler('click', null);
            expect(spy).toHaveBeenCalledWith(DATA_MOCK.option2.value);
        });
    });
});
