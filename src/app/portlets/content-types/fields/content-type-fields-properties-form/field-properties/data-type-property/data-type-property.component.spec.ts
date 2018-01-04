
import { DataTypePropertyComponent } from './';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { MockDotMessageService } from '../../../../../../test/dot-message-service.mock';
import { async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotMessageService } from '../../../../../../api/services/dot-messages-service';
import { FormGroup, FormControl } from '@angular/forms';
import { By } from '@angular/platform-browser';

describe('DataTypePropertyComponent', () => {
    let comp: DataTypePropertyComponent;
    let fixture: ComponentFixture<DataTypePropertyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.data_type.label': 'Data-Type',
        'contenttypes.field.properties.data_type.values.binary': 'Binary',
        'contenttypes.field.properties.data_type.values.text': 'Text',
        'contenttypes.field.properties.data_type.values.boolean': 'True-False',
        'contenttypes.field.properties.data_type.values.date': 'Date',
        'contenttypes.field.properties.data_type.values.decimal': 'Decimal',
        'contenttypes.field.properties.data_type.values.number': 'Whole-Number',
        'contenttypes.field.properties.data_type.values.large_text': 'Large-Block-of-Text',
        'contenttypes.field.properties.data_type.values.system': 'System-Field',
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                DataTypePropertyComponent
            ],
            imports: [
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
            ]
        });

        fixture = DOTTestBed.createComponent(DataTypePropertyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;

        this.group = new FormGroup({
            name: new FormControl('')
        });

        comp.group = this.group;
        comp.property = {
            field: {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
            },
            name: 'name',
            value: 'value'
        };
    }));

    it('should have a form', () => {
        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(this.group).toEqual(divForm.componentInstance.group);
    });

    it('should have 4 values for Radio Field', () => {

        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pRadioButtons = fixture.debugElement.queryAll(By.css('p-radioButton'));

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

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pRadioButtons = fixture.debugElement.queryAll(By.css('p-radioButton'));

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

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pRadioButtons = fixture.debugElement.queryAll(By.css('p-radioButton'));

        expect(3).toEqual(pRadioButtons.length);
        expect('Text').toBe(pRadioButtons[0].componentInstance.label);
        expect('TEXT').toBe(pRadioButtons[0].componentInstance.value);
        expect('Decimal').toBe(pRadioButtons[1].componentInstance.label);
        expect('FLOAT').toBe(pRadioButtons[1].componentInstance.value);
        expect('Whole-Number').toBe(pRadioButtons[2].componentInstance.label);
        expect('INTEGER').toBe(pRadioButtons[2].componentInstance.value);
    });
});
