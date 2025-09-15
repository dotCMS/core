import { DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DataTypePropertyComponent } from '.';

import { DOTTestBed } from '../../../../../../../../test/dot-test-bed';

describe('DataTypePropertyComponent', () => {
    let comp: DataTypePropertyComponent;
    let fixture: ComponentFixture<DataTypePropertyComponent>;
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.data_type.label': 'Data-Type',
        'contenttypes.field.properties.data_type.values.binary': 'Binary',
        'contenttypes.field.properties.data_type.values.text': 'Text',
        'contenttypes.field.properties.data_type.values.boolean': 'True-False',
        'contenttypes.field.properties.data_type.values.date': 'Date',
        'contenttypes.field.properties.data_type.values.decimal': 'Decimal',
        'contenttypes.field.properties.data_type.values.number': 'Whole-Number',
        'contenttypes.field.properties.data_type.values.large_text': 'Large-Block-of-Text',
        'contenttypes.field.properties.data_type.values.system': 'System-Field'
    });

    let group;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DataTypePropertyComponent],
            imports: [DotMessagePipe],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(DataTypePropertyComponent);
        comp = fixture.componentInstance;

        group = new UntypedFormGroup({
            name: new UntypedFormControl('')
        });

        comp.group = group;
        comp.property = {
            field: {
                ...dotcmsContentTypeFieldBasicMock,
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField'
            },
            name: 'name',
            value: 'value'
        };
    }));

    it('should have a form', () => {
        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have 4 values for Radio Field', () => {
        fixture.detectChanges();

        const pRadioButtons = fixture.debugElement.queryAll(By.css('p-radiobutton'));

        expect(4).toEqual(pRadioButtons.length);
        expect('Text').toBe(pRadioButtons[0].componentInstance.label);
        expect('TEXT').toBe(pRadioButtons[0].componentInstance.value);
        expect('True-False').toBe(pRadioButtons[1].componentInstance.label);
        expect('BOOL').toBe(pRadioButtons[1].componentInstance.value);
        expect('Decimal').toBe(pRadioButtons[2].componentInstance.label);
        expect('FLOAT').toBe(pRadioButtons[2].componentInstance.value);
        expect('Whole-Number').toBe(pRadioButtons[3].componentInstance.label);
        expect('INTEGER').toBe(pRadioButtons[3].componentInstance.value);
    });

    it('should have 4 values for Select Field', () => {
        comp.property.field.clazz = 'com.dotcms.contenttype.model.field.ImmutableSelectField';
        fixture.detectChanges();

        const pRadioButtons = fixture.debugElement.queryAll(By.css('p-radiobutton'));

        expect(4).toEqual(pRadioButtons.length);
        expect('Text').toBe(pRadioButtons[0].componentInstance.label);
        expect('TEXT').toBe(pRadioButtons[0].componentInstance.value);
        expect('True-False').toBe(pRadioButtons[1].componentInstance.label);
        expect('BOOL').toBe(pRadioButtons[1].componentInstance.value);
        expect('Decimal').toBe(pRadioButtons[2].componentInstance.label);
        expect('FLOAT').toBe(pRadioButtons[2].componentInstance.value);
        expect('Whole-Number').toBe(pRadioButtons[3].componentInstance.label);
        expect('INTEGER').toBe(pRadioButtons[3].componentInstance.value);
    });

    it('should have 4 values for Text Field', () => {
        comp.property.field.clazz = 'com.dotcms.contenttype.model.field.ImmutableTextField';
        fixture.detectChanges();

        const pRadioButtons = fixture.debugElement.queryAll(By.css('p-radiobutton'));

        expect(3).toEqual(pRadioButtons.length);
        expect('Text').toBe(pRadioButtons[0].componentInstance.label);
        expect('TEXT').toBe(pRadioButtons[0].componentInstance.value);
        expect('Decimal').toBe(pRadioButtons[1].componentInstance.label);
        expect('FLOAT').toBe(pRadioButtons[1].componentInstance.value);
        expect('Whole-Number').toBe(pRadioButtons[2].componentInstance.label);
        expect('INTEGER').toBe(pRadioButtons[2].componentInstance.value);
    });
});
