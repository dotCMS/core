import { ComponentFixture } from '@angular/core/testing';
import { DotPersonaSelectorOptionComponent } from './dot-persona-selector-option.component';
import { DebugElement } from '@angular/core';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-messages-service';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { By } from '@angular/platform-browser';
import { mockDotPersona } from '@tests/dot-persona.mock';

describe('DotPersonaSelectorOptionComponent', () => {

    let component: DotPersonaSelectorOptionComponent;
    let fixture: ComponentFixture<DotPersonaSelectorOptionComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        'modes.persona.personalized': 'Personalized'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotPersonaSelectorOptionComponent],
            imports: [BrowserAnimationsModule, DotIconModule, DotAvatarModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotPersonaSelectorOptionComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.persona = mockDotPersona;
        fixture.detectChanges();
    });

    it('should have dot-avatar with right properties', () => {
        const avatar: DebugElement = de.query(By.css('dot-avatar'));
        expect(avatar.componentInstance.label).toBe(mockDotPersona.name);
        expect(avatar.componentInstance.showDot).toBe(mockDotPersona.personalized);
        expect(avatar.componentInstance.url).toBe(mockDotPersona.photo);
        expect(avatar.componentInstance.size).toBe(24);
    });

    it('should label set personalized class', () => {
        const lblElement: DebugElement = de.query(By.css('.dot-persona-selector-option__label'));
        expect(lblElement.nativeElement.innerText).toBe(mockDotPersona.name);
        expect(lblElement.nativeElement.classList).toContain('dot-persona-selector-option__personalized');
    });

    it('should label not set personalized class', () => {
        component.persona = { ...mockDotPersona, personalized: false };
        fixture.detectChanges();

        const lblElement: DebugElement = de.query(By.css('.dot-persona-selector-option__label'));
        expect(lblElement.nativeElement.innerText).toBe(mockDotPersona.name);
        expect(lblElement.nativeElement.classList).not.toContain('dot-persona-selector-option__personalized');
    });

    it('should have personalized button with right properties', () => {
        const btnElement: DebugElement = de.query(By.css('button'));
        expect(btnElement.nativeElement.innerText.indexOf('Personalized'.toUpperCase())).toBe(0);
        expect(btnElement.attributes.icon).toBe('fa fa-times');
        expect(btnElement.attributes.iconPos).toBe('right');
    });

    it('should not display personalized button when no properties set', () => {
        component.persona = { ...mockDotPersona, personalized: false };
        fixture.detectChanges();

        const btnElement: DebugElement = de.query(By.css('button'));
        const dotIconElement: DebugElement = de.query(By.css('dot-icon'));
        expect(btnElement).toBeNull();
        expect(dotIconElement).toBeNull();
    });

    it('should emit persona when delete clicked', () => {
        spyOn(component.delete, 'emit');
        const btnElement: DebugElement = de.query(By.css('button'));
        btnElement.triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        expect(component.delete.emit).toHaveBeenCalledWith(mockDotPersona);
    });

    it('should emit persona when field clicked', () => {
        spyOn(component.selected, 'emit');
        de.triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        expect(component.selected.emit).toHaveBeenCalledWith(mockDotPersona);
    });
});
