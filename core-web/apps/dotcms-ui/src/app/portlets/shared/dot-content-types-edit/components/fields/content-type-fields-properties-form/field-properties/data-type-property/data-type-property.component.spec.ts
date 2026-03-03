import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
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
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
            schemas: [NO_ERRORS_SCHEMA]
        });

        fixture = DOTTestBed.createComponent(DataTypePropertyComponent);
        comp = fixture.componentInstance;
        const originalDetectChanges = fixture.detectChanges.bind(fixture);
        fixture.detectChanges = () => originalDetectChanges(false);

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
        const labelTexts = fixture.debugElement
            .queryAll(By.css('.flex label'))
            .map((label) => label.nativeElement.textContent.trim());

        expect(4).toEqual(pRadioButtons.length);
        expect(labelTexts).toEqual(['Text', 'True-False', 'Decimal', 'Whole-Number']);
        expect(pRadioButtons[0].componentInstance.value).toBe('TEXT');
        expect(pRadioButtons[1].componentInstance.value).toBe('BOOL');
        expect(pRadioButtons[2].componentInstance.value).toBe('FLOAT');
        expect(pRadioButtons[3].componentInstance.value).toBe('INTEGER');
    });

    it('should have 4 values for Select Field', () => {
        comp.property.field.clazz = 'com.dotcms.contenttype.model.field.ImmutableSelectField';
        comp.ngOnInit();
        fixture.detectChanges();

        const pRadioButtons = fixture.debugElement.queryAll(By.css('p-radiobutton'));
        const labelTexts = fixture.debugElement
            .queryAll(By.css('.flex label'))
            .map((label) => label.nativeElement.textContent.trim());

        expect(4).toEqual(pRadioButtons.length);
        expect(labelTexts).toEqual(['Text', 'True-False', 'Decimal', 'Whole-Number']);
        expect(pRadioButtons[0].componentInstance.value).toBe('TEXT');
        expect(pRadioButtons[1].componentInstance.value).toBe('BOOL');
        expect(pRadioButtons[2].componentInstance.value).toBe('FLOAT');
        expect(pRadioButtons[3].componentInstance.value).toBe('INTEGER');
    });

    it('should have 3 values for Text Field', () => {
        const textFixture = DOTTestBed.createComponent(DataTypePropertyComponent);
        const textComp = textFixture.componentInstance;
        const textGroup = new UntypedFormGroup({
            name: new UntypedFormControl('')
        });

        textComp.group = textGroup;
        textComp.property = {
            field: {
                ...dotcmsContentTypeFieldBasicMock,
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField'
            },
            name: 'name',
            value: 'value'
        };

        const originalDetectChanges = textFixture.detectChanges.bind(textFixture);
        textFixture.detectChanges = () => originalDetectChanges(false);
        textFixture.detectChanges();

        const pRadioButtons = textFixture.debugElement.queryAll(By.css('p-radiobutton'));
        const labelTexts = textFixture.debugElement
            .queryAll(By.css('.flex label'))
            .map((label) => label.nativeElement.textContent.trim());

        expect(3).toEqual(pRadioButtons.length);
        expect(labelTexts).toEqual(['Text', 'Decimal', 'Whole-Number']);
        expect(pRadioButtons[0].componentInstance.value).toBe('TEXT');
        expect(pRadioButtons[1].componentInstance.value).toBe('FLOAT');
        expect(pRadioButtons[2].componentInstance.value).toBe('INTEGER');
    });
});
