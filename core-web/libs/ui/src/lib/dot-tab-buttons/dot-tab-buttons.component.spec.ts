import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { NgClass, NgFor, NgIf } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotPageMode } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTabButtonsComponent } from './dot-tab-buttons.component';

import { DotMessagePipe } from '../dot-message/dot-message.pipe';

describe('DotTabButtonsComponent', () => {
    let spectator: Spectator<DotTabButtonsComponent>;
    const pointerEvent = new PointerEvent('click');
    let editID: string;
    let previewID: string;

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
        ],
        imports: [NgFor, ButtonModule, NgIf, NgClass, TooltipModule, DotMessagePipe]
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
        spectator.detectChanges();

        editID = spectator.component._options[0].value.id;
        previewID = spectator.component._options[1].value.id;
    });

    it('should render options', () => {
        const buttons = spectator.queryAll(byTestId('dot-tab-button-text'));
        expect(buttons[0]).toBeDefined();
        expect(buttons.length).toEqual(2);
        buttons.forEach((button, index) => {
            expect(button.textContent.trim()).toEqual(optionsMock[index].label);
        });
    });

    it('should emit openMenu when onClickDropdown is called with correct target', () => {
        const openMenuSpy = jest.spyOn(spectator.component.openMenu, 'emit');
        const tab = spectator.queryAll(byTestId('dot-tab-container'))[1] as HTMLElement;
        const button = spectator.fixture.debugElement.queryAll(
            By.css('[data-testId="dot-tab-button-text"]')
        )[1];

        spectator.component.onClickDropdown(
            { ...pointerEvent, target: button.nativeElement },
            previewID
        );
        expect(openMenuSpy).toHaveBeenCalledWith({
            event: { ...pointerEvent, target: button.nativeElement },
            menuId: previewID,
            target: tab
        });
    });

    it('should emit openMenu when dropdown button is clicked via template', () => {
        const openMenuSpy = jest.spyOn(spectator.component.openMenu, 'emit');
        const tab = spectator.queryAll(byTestId('dot-tab-container'))[1] as HTMLElement;

        const button = spectator.fixture.debugElement.queryAll(
            By.css('[data-testId="dot-tab-button-text"]')
        )[1];

        spectator.triggerEventHandler('[data-testId="dot-tab-button-dropdown"]', 'click', {
            ...pointerEvent,
            target: button.nativeElement
        });

        expect(openMenuSpy).toHaveBeenCalledWith({
            event: {
                ...pointerEvent,
                target: button.nativeElement
            },
            menuId: previewID,
            target: tab
        });
    });

    it('should not emit openMenu when onClickDropdown is called for option without showDropdownButton', () => {
        const openMenuSpy = jest.spyOn(spectator.component.openMenu, 'emit');
        spectator.component.onClickDropdown(pointerEvent, editID);
        const tab = spectator.queryAll(byTestId('dot-tab-container'))[1] as HTMLElement;

        expect(openMenuSpy).not.toHaveBeenCalledWith({
            event: pointerEvent,
            menuId: editID,
            target: tab
        });
    });

    it('should emit clickOption event when onClickOption is called with a PREVIEW value', () => {
        const clickOptionSpy = jest.spyOn(spectator.component.clickOption, 'emit');
        spectator.component.activeId = DotPageMode.EDIT;

        const button = spectator.fixture.debugElement.queryAll(
            By.css('[data-testId="dot-tab-button-text"]')
        )[1];

        spectator.triggerEventHandler(button, 'click', pointerEvent);

        expect(clickOptionSpy).toHaveBeenCalledWith({
            event: pointerEvent,
            optionId: previewID
        });
    });

    it('should not emit clickOption event when onClickOption is called if the user is in the same tab', () => {
        const clickOptionSpy = jest.spyOn(spectator.component.clickOption, 'emit');
        spectator.component.activeId = DotPageMode.PREVIEW;

        const buttons = spectator.queryAll(byTestId('dot-tab-button-text'));
        spectator.click(buttons[1]);

        expect(clickOptionSpy).not.toHaveBeenCalled();
    });

    it('should call showMenu when onClickDropdown is called ', () => {
        const openMenuSpy = jest.spyOn(spectator.component.openMenu, 'emit');
        const tab = spectator.queryAll(byTestId('dot-tab-container'))[1] as HTMLElement;
        const dropdownButton = spectator.fixture.debugElement.query(
            By.css('[data-testId="dot-tab-button-dropdown"]')
        );

        spectator.triggerEventHandler(dropdownButton, 'click', {
            ...pointerEvent,
            target: dropdownButton.nativeElement
        });

        expect(openMenuSpy).toHaveBeenCalledWith({
            event: {
                ...pointerEvent,
                target: dropdownButton.nativeElement
            },
            menuId: previewID,
            target: tab
        });
    });

    it('should show dot-tab-indicator for the active tab', () => {
        const indicatorEl = spectator.queryAll(byTestId('dot-tab-button'));
        expect(indicatorEl).toBeDefined();
        expect(indicatorEl.length).toBe(1);
    });

    it('should display closed dropdown icon when dropdown is closed', () => {
        spectator.detectChanges();

        const icon = spectator.query(byTestId('dot-tab-icon'));
        expect(icon?.classList.contains('pi-angle-down')).toBe(true);
    });

    it('should set all dropdown toggles to false when resetDropdowns is called', () => {
        spectator.component.onClickDropdown(pointerEvent, previewID);

        expect(spectator.component._options[1].value.toggle).toBe(true);

        spectator.component.resetDropdowns();

        expect(spectator.component._options[1].value.toggle).toBe(false);
        expect(spectator.component._options.every((o) => o.value.toggle !== true)).toBe(true);
    });

    it('should set the given dropdown toggle to false when resetDropdownById is called', () => {
        spectator.component.onClickDropdown(pointerEvent, previewID);

        expect(spectator.component._options[1].value.toggle).toBe(true);

        spectator.component.resetDropdownById(previewID);

        expect(spectator.component._options[1].value.toggle).toBe(false);
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
            expect(spectator.queryAll(byTestId('dot-tab-button-dropdown')).length).toBe(2);
        });

        it('should have 4 tabs', () => {
            expect(spectator.queryAll(byTestId('dot-tab-button-text')).length).toBe(4);
        });
    });
});
