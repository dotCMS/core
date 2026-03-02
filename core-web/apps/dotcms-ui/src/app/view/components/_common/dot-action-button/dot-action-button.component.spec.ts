import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { MenuItem } from 'primeng/api';
import { Button } from 'primeng/button';
import { Menu } from 'primeng/menu';

import { DotActionButtonComponent } from './dot-action-button.component';

describe('DotActionButtonComponent', () => {
    let spectator: Spectator<DotActionButtonComponent>;
    const createComponent = createComponentFactory({
        component: DotActionButtonComponent,
        imports: [BrowserAnimationsModule, RouterTestingModule]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should have no-label class by default', () => {
        expect(spectator.element.classList).toContain('action-button--no-label');
    });

    it('should NOT have no-label class when label is set', () => {
        spectator.setInput('label', 'Hello World');
        expect(spectator.element.classList).not.toContain('action-button--no-label');
    });

    it('should have only button in default state', () => {
        expect(spectator.query('p-button')).toExist();
        expect(spectator.query(byTestId('dot-action-button-label'))).not.toExist();
        expect(spectator.query('p-menu')).not.toExist();
    });

    it('should have label', () => {
        spectator.setInput('label', 'Hello World');
        expect(spectator.query(byTestId('dot-action-button-label'))).toHaveText('Hello World');
    });

    it('should have p-menu and pass the model to it', () => {
        const model: MenuItem[] = [
            {
                command: () => {
                    //
                },
                icon: 'whatever',
                label: 'Whatever'
            }
        ];

        spectator.setInput('model', model);
        const menu = spectator.query(Menu);
        expect(menu).toExist();
        expect(menu.model).toEqual(model);
    });

    it('should emit event on button click', () => {
        const pressSpy = jest.spyOn(spectator.component.press, 'emit');
        spectator.click(byTestId('dot-action-button'));
        expect(pressSpy).toHaveBeenCalled();
    });

    it('should toggle the menu on button click', () => {
        const model: MenuItem[] = [
            {
                command: () => {
                    //
                },
                icon: 'whatever',
                label: 'Whatever'
            }
        ];

        spectator.setInput('model', model);
        const toggleSpy = jest.spyOn(spectator.component.$menu()!, 'toggle');

        spectator.click(byTestId('dot-action-button'));
        expect(toggleSpy).toHaveBeenCalledTimes(1);
    });

    it('should set button to disabled state', () => {
        spectator.setInput('disabled', true);
        spectator.setInput('label', 'Label');

        const button = spectator.query(Button);
        const label = spectator.query(byTestId('dot-action-button-label'));

        expect(button.disabled).toBe(true);
        expect(label).toHaveClass('text-gray-400');
        expect(label).toHaveClass('cursor-not-allowed');
    });
});
