import { HintPropertyComponent } from './index';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { FormGroup, FormControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';

describe('HintPropertyComponent', () => {
    let comp: HintPropertyComponent;
    let fixture: ComponentFixture<HintPropertyComponent>;
    const messageServiceMock = new MockDotMessageService({
        Hint: 'Hint'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [HintPropertyComponent],
            imports: [],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(HintPropertyComponent);
        comp = fixture.componentInstance;
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
            field: {
                ...dotcmsContentTypeFieldBasicMock
            }
        };

        fixture.detectChanges();

        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));

        expect(pInput).not.toBeNull();
    });
});
