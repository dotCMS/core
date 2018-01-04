import { HintPropertyComponent } from './index';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { MockDotMessageService } from '../../../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DotMessageService } from '../../../../../../api/services/dot-messages-service';
import { FormGroup, FormControl } from '@angular/forms';
import { By } from '@angular/platform-browser';


describe('HintPropertyComponent', () => {
    let comp: HintPropertyComponent;
    let fixture: ComponentFixture<HintPropertyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const messageServiceMock = new MockDotMessageService({
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
                { provide: DotMessageService, useValue: messageServiceMock },
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
