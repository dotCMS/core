import { DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { CheckboxPropertyComponent } from '.';

import { DOTTestBed } from '../../../../../../../../test/dot-test-bed';

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

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [CheckboxPropertyComponent],
            imports: [DotMessagePipe],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(CheckboxPropertyComponent);
        comp = fixture.componentInstance;
    }));

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
