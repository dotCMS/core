import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { SelectItem } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotPageMode } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTabButtonsComponent } from './dot-tab-buttons.component';

describe('DotTabButtonsComponent', () => {
    let spectator: Spectator<DotTabButtonsComponent>;
    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.edit.page.clipboard': 'Edit Page',
        'editpage.toolbar.preview.page.clipboard': 'Preview Page'
    });
    const createComponent = createComponentFactory({
        component: DotTabButtonsComponent,
        providers: [
            HttpClientTestingModule,
            DotMessagePipe,
            { provide: DotMessageService, useValue: messageServiceMock }
        ]
    });
    const optionsMock: SelectItem[] = [
        { label: 'Edit', value: DotPageMode.EDIT },
        { label: 'Preview', value: DotPageMode.PREVIEW }
    ];

    beforeEach(() => {
        spectator = createComponent({
            props: {
                options: optionsMock,
                mode: DotPageMode.PREVIEW,
                dotTabsIcon: 'pi-angle-down'
            }
        });
    });

    it('should render options', () => {
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
        spectator.component.mode = DotPageMode.EDIT;
        spectator.component.onClickOption({ target: { value: DotPageMode.PREVIEW } });
        expect(clickOptionSpy).toHaveBeenCalled();
    });

    it('should not emit clickOption event when onClickOption is called if the user is in the same tab', () => {
        const clickOptionSpy = spyOn(spectator.component.clickOption, 'emit');
        spectator.component.onClickOption({ target: { value: DotPageMode.PREVIEW } });
        expect(clickOptionSpy).not.toHaveBeenCalled();
    });

    it('should call showMenu when onClickOption is called with OPEN_MENU value', () => {
        const showMenuSpy = spyOn(spectator.component, 'showMenu');
        spectator.component.onClickOption({ target: { value: spectator.component.OPEN_MENU } });
        expect(showMenuSpy).toHaveBeenCalled();
    });

    it('should show dot-tab-indicator when a tab is active', () => {
        spectator.component.mode = DotPageMode.EDIT;

        const indicatorEl = spectator.queryAll(byTestId('dot-tab-indicator'));

        expect(indicatorEl).toBeDefined();
    });

    it('should show dot-tab-indicator when a tab is active', () => {
        spectator.component.mode = DotPageMode.PREVIEW;

        const indicatorEl = spectator.queryAll(byTestId('dot-tab-indicator'));

        expect(indicatorEl).toBeDefined();
    });

    it('should toggle icon', () => {
        const dotTabsIcon = spectator.queryAll(byTestId('dot-tab-icon'));

        spectator.setInput({
            dotTabsIcon: 'pi-angle-up'
        });
        spectator.detectChanges();
        expect(dotTabsIcon).toHaveClass('dot-tab-dropdown__icon pi pi-angle-up');

        spectator.setInput({
            dotTabsIcon: 'pi-angle-down'
        });

        spectator.fixture.detectChanges();

        expect(dotTabsIcon).toHaveClass('pi-angle-down');
    });
});
