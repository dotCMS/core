import { HintPropertyComponent } from './index';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockMessageService } from '../../../../../../test/message-service.mock';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { MessageService } from '../../../../../../api/services/messages-service';
import { FormGroup, FormControl, NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';


describe('HintPropertyComponent', () => {
    let comp: HintPropertyComponent;
    let fixture: ComponentFixture<HintPropertyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const messageServiceMock = new MockMessageService({
        'Hint': 'Hint'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                HintPropertyComponent
            ],
            imports: [
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
            ]
        });

        fixture = DOTTestBed.createComponent(HintPropertyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should have a form', () => {
        const group = new FormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a input', () => {
        comp.group = new FormGroup({
            name: new FormControl('')
        });
        comp.property = {
            name: 'name',
            value: 'value',
            field: {}
        };

        fixture.detectChanges();

        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));

        expect(pInput).not.toBeNull();
    });
});
