import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule, NgClass, NgIf } from '@angular/common';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { DotPageMode } from '@dotcms/dotcms-models';

import { DotTabButtonsComponent } from './dot-tab-buttons.component';

describe('DotTabButtonsComponent', () => {
    let spectator: Spectator<DotTabButtonsComponent>;

    const createComponent = createComponentFactory({
        component: DotTabButtonsComponent,
        imports: [CommonModule, ButtonModule, NgIf, NgClass]
    });
    const optionsMock: SelectItem[] = [
        { label: 'Edit', value: DotPageMode.EDIT },
        { label: 'Preview', value: DotPageMode.PREVIEW }
    ];

    beforeEach(() => {
        spectator = createComponent({
            props: {
                options: optionsMock,
                mode: DotPageMode.PREVIEW
            }
        });
    });

    it('should render options', () => {
        spectator.detectChanges();

        const buttons = spectator.queryAll(byTestId('dot-tab-button-text'));
        expect(spectator.query(byTestId('dot-tab-button-text'))).toBeDefined();
        expect(buttons.length).toEqual(2);
        buttons.forEach((button, index) => {
            expect(button.textContent.trim()).toEqual(optionsMock[index].label);
        });
    });

    it('should emit openMenu event when showMenu is called', () => {
        const openMenuSpy = spyOn(spectator.component.openMenu, 'emit');
        spectator.component.showMenu(null);
        expect(openMenuSpy).toHaveBeenCalled();
    });

    it('should emit clickOption event when onClickOption is called with a PREVIEW value', () => {
        const clickOptionSpy = spyOn(spectator.component.clickOption, 'emit');
        spectator.component.onClickOption({ target: { value: DotPageMode.PREVIEW } });
        expect(clickOptionSpy).toHaveBeenCalled();
    });

    it('should call showMenu when onClickOption is called with OPEN_MENU value', () => {
        const showMenuSpy = spyOn(spectator.component, 'showMenu');
        spectator.component.onClickOption({ target: { value: spectator.component.OPEN_MENU } });
        expect(showMenuSpy).toHaveBeenCalled();
    });

    it('should show dot-tab-indicator when a tab is active', () => {
        spectator.component.mode = DotPageMode.EDIT;

        spectator.detectChanges();

        const indicatorEl = spectator.queryAll(byTestId('dot-tab-indicator'));

        expect(indicatorEl).toBeDefined();
    });

    it('should show dot-tab-indicator when a tab is active', () => {
        spectator.component.mode = DotPageMode.PREVIEW;

        spectator.detectChanges();

        const indicatorEl = spectator.queryAll(byTestId('dot-tab-indicator'));

        expect(indicatorEl).toBeDefined();
    });
});
