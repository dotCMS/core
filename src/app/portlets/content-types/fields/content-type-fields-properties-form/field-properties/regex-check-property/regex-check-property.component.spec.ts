import { FormGroup, FormControl, NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { RegexCheckPropertyComponent } from './index';
import { ComponentFixture, async, fakeAsync, tick } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockMessageService } from '../../../../../../test/message-service.mock';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { MessageService } from '../../../../../../api/services/messages-service';

describe('RegexCheckPropertyComponent', () => {
    let comp: RegexCheckPropertyComponent;
    let fixture: ComponentFixture<RegexCheckPropertyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const messageServiceMock = new MockMessageService({
        'contenttypes.field.properties.validation_regex.label': 'Validation-RegEx',
        'contenttypes.field.properties.validation_regex.values.select': 'Select',
        'contenttypes.field.properties.validation_regex.values.email': 'Email',
        'contenttypes.field.properties.validation_regex.values.numbers_only': 'Numbers only',
        'contenttypes.field.properties.validation_regex.values.letters_only': 'Letters only',
        'contenttypes.field.properties.validation_regex.values.alphanumeric': 'Alphanumeric',
        'contenttypes.field.properties.validation_regex.values.us_zip_code': 'US Zip Code',
        'contenttypes.field.properties.validation_regex.values.us_phone': 'US Phone',
        'contenttypes.field.properties.validation_regex.values.url_pattern': 'URL Pattern',
        'contenttypes.field.properties.validation_regex.values.no_html': 'No HTML',
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                RegexCheckPropertyComponent
            ],
            imports: [
                NoopAnimationsModule
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
            ]
        });

        fixture = DOTTestBed.createComponent(RegexCheckPropertyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;

        comp.group = new FormGroup({
            regexCheck: new FormControl('')
        });
        comp.property = {
            name: 'regexCheck',
            value: 'value',
            field: {}
        };
    }));

    it('should have a form', () => {
        const group = new FormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a input', () => {
        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));

        expect(pInput).not.toBeNull();
    });

    it('should have a dropDown', () => {
        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pDropDown: DebugElement = fixture.debugElement.query(By.css('p-dropdown'));

        expect(pDropDown).not.toBeNull();
        expect(comp.regexCheckTempletes).toBe(pDropDown.componentInstance.options);
    });

    it('should change the input value', () => {
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pDropDown: DebugElement = fixture.debugElement.query(By.css('p-dropdown'));

        pDropDown.triggerEventHandler('onChange', {
            value: '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4})$'
        });

        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));
        expect('^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4})$').toBe(comp.group.get('regexCheck').value);
    });
});
