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
        { label: 'Edit', value: { id: DotPageMode.EDIT, showDropdownButton: false } },
        { label: 'Preview', value: { id: DotPageMode.PREVIEW, showDropdownButton: true } }
    ];

    beforeEach(() => {
        spectator = createComponent({
            props: {
                options: optionsMock,
                activeId: DotPageMode.PREVIEW
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
        const openMenuSpy = spyOn(spectator.component.dropdownClick, 'emit');
        // spectator.component.showMenu(null);
        expect(openMenuSpy).toHaveBeenCalled();
    });

    it('should emit clickOption event when onClickOption is called with a PREVIEW value', () => {
        const clickOptionSpy = spyOn(spectator.component.clickOption, 'emit');
        spectator.component.activeId = DotPageMode.EDIT;

        // spectator.component.onClickOption({
        //     ...pointerEvent,
        //     target: {
        //         ...pointerEvent.target,
        //         value: DotPageMode.PREVIEW
        //     }
        // });
        expect(clickOptionSpy).toHaveBeenCalled();
    });

    it('should not emit clickOption event when onClickOption is called if the user is in the same tab', () => {
        const clickOptionSpy = spyOn(spectator.component.clickOption, 'emit');
        // spectator.component.onClickOption({
        //     ...pointerEvent,
        //     target: {
        //         ...pointerEvent.target,
        //         value: DotPageMode.PREVIEW
        //     }
        // });
        expect(clickOptionSpy).not.toHaveBeenCalled();
    });

    it('should call showMenu when onClickOption is called with OPEN_MENU value', () => {
        // const showMenuSpy = spyOn(spectator.component, 'showMenu');
        // spectator.component.onClickOption({
        //     ...pointerEvent,
        //     target: {
        //         ...pointerEvent.target,
        //         value: DotPageMode.PREVIEW + spectator.component.OPEN_MENU
        //     }
        // });
        // expect(showMenuSpy).toHaveBeenCalled();
    });

    it('should show dot-tab-indicator when a tab is active', () => {
        spectator.component.activeId = DotPageMode.EDIT;

        const indicatorEl = spectator.queryAll(byTestId('dot-tab-indicator'));

        expect(indicatorEl).toBeDefined();
    });

    it('should show dot-tab-indicator when a tab is active', () => {
        spectator.component.activeId = DotPageMode.PREVIEW;

        const indicatorEl = spectator.queryAll(byTestId('dot-tab-indicator'));

        expect(indicatorEl).toBeDefined();
    });

    it('should toggle and change all dropdowns icon to original state on document click', () => {
        // spectator.component.onClickOption({
        //     ...pointerEvent,
        //     target: {
        //         ...pointerEvent.target,
        //         value: DotPageMode.PREVIEW + spectator.component.OPEN_MENU
        //     }
        // });

        expect(spectator.component._options[1].value.toggle).toBe(true);
        expect(spectator.component._options[1].value.icon).toBe('pi pi-angle-up');

        document.dispatchEvent(new Event('click'));

        expect(spectator.component._options[1].value.toggle).toBe(false);
        expect(spectator.component._options[1].value.icon).toBe('pi pi-angle-down');
    });

    describe('N tab buttons with and without dropdowns', () => {
        beforeEach(() => {
            spectator.setInput({
                options: [
                    { label: 'test-1', value: { id: 'test-1', showDropdownButton: false } },
                    { label: 'test-2', value: { id: 'test-2', showDropdownButton: false } },
                    { label: 'test-3', value: { id: 'test-3', showDropdownButton: true } },
                    { label: 'test-4', value: { id: 'test-4', showDropdownButton: true } }
                ]
            });
        });

        it('should have 2 tab dropdowns', () => {
            expect(spectator.queryAll(byTestId('tab-dropdown')).length).toBe(2);
        });

        it('should have 4 tabs', () => {
            expect(spectator.queryAll(byTestId('dot-tab-button-text')).length).toBe(4);
        });

        it('should toggle icon', () => {
            /* */
        });
    });
});
