import { CheckboxPropertyComponent } from '.';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';

describe('CheckboxPropertyComponent', () => {
    let comp: CheckboxPropertyComponent;
    let fixture: ComponentFixture<CheckboxPropertyComponent>;
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.required.label': 'required',
        'contenttypes.field.properties.user_searchable.label': 'user searchable.',
        'contenttypes.field.properties.system_indexed.label': 'system indexed',
        'contenttypes.field.properties.listed.label': 'listed',
        'contenttypes.field.properties.unique.label': 'unique'
    });

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [CheckboxPropertyComponent],
                imports: [],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            });

            fixture = DOTTestBed.createComponent(CheckboxPropertyComponent);
            comp = fixture.componentInstance;
        })
    );

    it('should have a form', () => {
        const group = new UntypedFormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a p-checkbox', () => {
        comp.group = new UntypedFormGroup({
            indexed: new UntypedFormControl('')
        });
        comp.property = {
            name: 'indexed',
            value: 'value',
            field: {
                ...dotcmsContentTypeFieldBasicMock
            }
        };

        fixture.detectChanges();

        const pCheckbox: DebugElement = fixture.debugElement.query(By.css('p-checkbox'));

        expect(pCheckbox).not.toBeNull();
        expect('system indexed').toBe(pCheckbox.componentInstance.label);
        expect('value').toBe(pCheckbox.componentInstance.value);
    });
});
