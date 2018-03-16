import { ComponentFixture } from '@angular/core/testing';
import { DotPersonaSelectorComponent } from './dot-persona-selector.component';
import { DebugElement } from '@angular/core';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { DotPersonasService } from '../../../api/services/dot-personas/dot-personas.service';
import { DotPersonasServiceMock } from '../../../test/dot-personas-service.mock';
import { By } from '@angular/platform-browser';
import { DotPersona } from '../../../shared/models/dot-persona/dot-persona.model';

describe('DotPersonaSelectorComponent', () => {
    let component: DotPersonaSelectorComponent;
    let fixture: ComponentFixture<DotPersonaSelectorComponent>;
    let de: DebugElement;
    const defaultPersona: DotPersona = { name: 'Default Persona', identifier: '0' };
    const messageServiceMock = new MockDotMessageService({
        'modes.persona.no.persona': 'Default Persona'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotPersonaSelectorComponent],
            imports: [BrowserAnimationsModule],
            providers: [
                {
                    provide: DotPersonasService,
                    useClass: DotPersonasServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotPersonaSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should emmit the selected persona', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));

        spyOn(component.selected, 'emit');
        spyOn(component, 'change').and.callThrough();

        pDropDown.triggerEventHandler('onChange', { value: defaultPersona });

        expect(component.change).toHaveBeenCalledWith(defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
    });

    it('should add Default persona as first position', () => {
        fixture.detectChanges();
        component.personasOptions.subscribe((personas: DotPersona[]) => {
            expect(personas[0]).toEqual(defaultPersona);
        });
    });
});
