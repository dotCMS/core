import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {
    NgControl,
    ReactiveFormsModule,
    UntypedFormControl,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { NamePropertyComponent } from './index';

@Component({
    selector: 'dot-field-validation-message',
    template: ''
})
class TestFieldValidationMessageComponent {
    @Input()
    field: NgControl;
    @Input()
    message: string;
}

describe('NamePropertyComponent', () => {
    let comp: NamePropertyComponent;
    let fixture: ComponentFixture<NamePropertyComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'Default-Value': 'Default-Value'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [NamePropertyComponent, TestFieldValidationMessageComponent],
            imports: [DotCopyLinkModule, ReactiveFormsModule, DotSafeHtmlPipe, DotMessagePipe],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();

        fixture = TestBed.createComponent(NamePropertyComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;

        comp.property = {
            name: 'name',
            value: 'value',
            field: {
                ...dotcmsContentTypeFieldBasicMock
            }
        };
    }));

    it('should have a form', () => {
        const group = new UntypedFormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a input', () => {
        comp.group = new UntypedFormGroup({
            name: new UntypedFormControl('')
        });

        fixture.detectChanges();

        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));

        expect(pInput).not.toBeNull();
    });

    it('should have a field-message', () => {
        comp.group = new UntypedFormGroup({
            name: new UntypedFormControl('')
        });

        fixture.detectChanges();

        const fieldValidationmessage: DebugElement = fixture.debugElement.query(
            By.css('dot-field-validation-message')
        );

        expect(fieldValidationmessage).not.toBeNull();
        expect(comp.group.controls['name']).toBe(fieldValidationmessage.componentInstance.field);
    });

    it('should focus on input on load using the directive', () => {
        const input = de.query(By.css('.name__input'));
        expect(input.attributes.dotAutofocus).toBeDefined();
    });

    it('should have copy variable button', () => {
        comp.group = new UntypedFormGroup({
            name: new UntypedFormControl('')
        });

        comp.property = {
            name: 'name',
            value: 'value',
            field: {
                ...dotcmsContentTypeFieldBasicMock,
                variable: 'thisIsAVar'
            }
        };

        fixture.detectChanges();

        const copy: DebugElement = de.query(By.css('dot-copy-link'));

        expect(copy.componentInstance.copy).toBe('thisIsAVar');
        expect(copy.componentInstance.label).toBe('thisIsAVar');
    });
});
