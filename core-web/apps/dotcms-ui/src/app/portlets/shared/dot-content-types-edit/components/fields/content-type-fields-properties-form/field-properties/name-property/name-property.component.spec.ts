import { NamePropertyComponent } from './index';
import { ComponentFixture, waitForAsync, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { UntypedFormGroup, UntypedFormControl, NgControl, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';
import { DotPipesModule } from '@pipes/dot-pipes.module';

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

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [NamePropertyComponent, TestFieldValidationMessageComponent],
                imports: [DotCopyLinkModule, ReactiveFormsModule, DotPipesModule],
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
        })
    );

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
