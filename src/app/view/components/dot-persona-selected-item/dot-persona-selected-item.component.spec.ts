import { ComponentFixture } from '@angular/core/testing';
import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { By } from '@angular/platform-browser';
import { mockDotPersona } from '@tests/dot-persona.mock';

describe('DotPersonaDropdownSelectorComponent', () => {

    let component: DotPersonaSelectedItemComponent;
    let fixture: ComponentFixture<DotPersonaSelectedItemComponent>;
    let de: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotPersonaSelectedItemComponent],
            imports: [BrowserAnimationsModule, DotIconModule, DotAvatarModule],
        });

        fixture = DOTTestBed.createComponent(DotPersonaSelectedItemComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.persona = mockDotPersona;
        component.label = 'Titulo';
        fixture.detectChanges();
    });

    it('should have dot-avatar with right properties', () => {
        const avatar: DebugElement = de.query(By.css('dot-avatar'));
        expect(avatar.componentInstance.label).toBe(mockDotPersona.name);
        expect(avatar.componentInstance.showDot).toBe(mockDotPersona.personalized);
        expect(avatar.componentInstance.url).toBe(mockDotPersona.photo);
        expect(avatar.componentInstance.size).toBe(32);
    });

    it('should render persona name and label', () => {
        const label = de.query(By.css('.dot-persona-selector__label')).nativeElement;
        const name = de.query(By.css('.dot-persona-selector__name')).nativeElement;
        expect(label.innerText).toBe('Titulo');
        expect(name.innerText).toBe('Global Investor');
    });

    it('should emit event when selected clicked', () => {
        spyOn(component.selected, 'emit');
        de.triggerEventHandler('click', {
            stopPropagation: () => {}
        });
        expect(component.selected.emit).toHaveBeenCalled();
    });
});
