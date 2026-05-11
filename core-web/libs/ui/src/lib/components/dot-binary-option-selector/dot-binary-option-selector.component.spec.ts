import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    BINARY_OPTION,
    DotBinaryOptionSelectorComponent
} from './dot-binary-option-selector.component';

import { DotAutofocusDirective } from '../../directives/dot-autofocus/dot-autofocus.directive';
import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

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
    let dynamicDialogRef: DynamicDialogRef;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotBinaryOptionSelectorComponent, DotMessagePipe, DotAutofocusDirective],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jest.fn()
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

        it('should render as a button card', () => {
            expect(option1).toBeTruthy();
            expect(option1.nativeElement.tagName).toBe('BUTTON');
            expect(component.firstOption.value).toEqual(DATA_MOCK.option1.value);
        });

        it('should have icon', () => {
            const icon = option1.query(By.css('.material-symbols-outlined'));
            expect(icon).toBeTruthy();
            expect(icon.nativeElement.textContent.trim()).toBe(DATA_MOCK.option1.icon);
        });

        it('should have label', () => {
            const label = option1.query(By.css('[data-testId="label-op1"]'));
            expect(label.nativeElement.innerText.trim()).toBe(
                messageServiceMock.get(DATA_MOCK.option1.label)
            );
        });

        it('should select option 1 when clicked', () => {
            // First flip selection to option 2 so we can verify the click changes it back.
            option1.nativeElement.click();
            fixture.detectChanges();
            expect(component.value).toBe(DATA_MOCK.option1.value);

            de.query(By.css('[data-testId="option2"]')).nativeElement.click();
            fixture.detectChanges();
            expect(component.value).toBe(DATA_MOCK.option2.value);

            option1.nativeElement.click();
            fixture.detectChanges();
            expect(component.value).toBe(DATA_MOCK.option1.value);
        });
    });

    describe('Option 2', () => {
        let option2: DebugElement;

        beforeEach(() => {
            option2 = de.query(By.css('[data-testId="option2"]'));
        });

        it('should render as a button card', () => {
            expect(option2).toBeTruthy();
            expect(option2.nativeElement.tagName).toBe('BUTTON');
            expect(component.secondOption.value).toEqual(DATA_MOCK.option2.value);
        });

        it('should have icon', () => {
            const icon = option2.query(By.css('.material-symbols-outlined'));
            expect(icon).toBeTruthy();
            expect(icon.nativeElement.textContent.trim()).toBe(DATA_MOCK.option2.icon);
        });

        it('should have label', () => {
            const label = option2.query(By.css('[data-testId="label-op2"]'));
            expect(label.nativeElement.innerText.trim()).toBe(
                messageServiceMock.get(DATA_MOCK.option2.label)
            );
        });

        it('should select option 2 when clicked', () => {
            option2.nativeElement.click();
            fixture.detectChanges();
            expect(component.value).toBe(DATA_MOCK.option2.value);
        });
    });

    describe('selected state', () => {
        it('should mark option1 as selected by default', () => {
            const option1 = de.query(By.css('[data-testId="option1"]'));
            expect(option1.nativeElement.classList).toContain('border-primary-500');
            expect(option1.nativeElement.classList).toContain('bg-primary-50');
        });

        it('should move the selected styles when the value changes', () => {
            de.query(By.css('[data-testId="option2"]')).nativeElement.click();
            fixture.detectChanges();

            const option1 = de.query(By.css('[data-testId="option1"]'));
            const option2 = de.query(By.css('[data-testId="option2"]'));
            expect(option2.nativeElement.classList).toContain('border-primary-500');
            expect(option1.nativeElement.classList).not.toContain('border-primary-500');
        });
    });

    describe('button', () => {
        it('should close dialog with the default selected value (option 1)', () => {
            const button = de.query(By.css('[data-testId="button"]'));
            const spy = jest.spyOn(dynamicDialogRef, 'close');
            button.triggerEventHandler('click', null);
            expect(spy).toHaveBeenCalledWith(DATA_MOCK.option1.value);
        });

        it('should close dialog with the active selection after switching options', () => {
            de.query(By.css('[data-testId="option2"]')).nativeElement.click();
            fixture.detectChanges();

            const button = de.query(By.css('[data-testId="button"]'));
            const spy = jest.spyOn(dynamicDialogRef, 'close');
            button.triggerEventHandler('click', null);
            expect(spy).toHaveBeenCalledWith(DATA_MOCK.option2.value);
        });
    });
});
