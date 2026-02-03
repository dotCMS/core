import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { SplitButton, SplitButtonModule } from 'primeng/splitbutton';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { ActionHeaderComponent } from './action-header.component';

import { DotActionButtonComponent } from '../../_common/dot-action-button/dot-action-button.component';

describe('ActionHeaderComponent', () => {
    let spectator: Spectator<ActionHeaderComponent>;

    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(), // deprecated
            removeListener: jest.fn(), // deprecated
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn()
        }))
    });

    const messageServiceMock = new MockDotMessageService({
        selected: 'selected'
    });

    const createComponent = createComponentFactory({
        component: ActionHeaderComponent,
        imports: [
            BrowserAnimationsModule,
            RouterTestingModule,
            DotActionButtonComponent,
            SplitButtonModule
        ],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DotAlertConfirmService, useValue: {} }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render default state correctly', () => {
        expect(spectator.query('dot-action-button')).not.toExist();
        expect(spectator.query('p-splitButton')).not.toExist();
    });

    it('should show the number of items selected', () => {
        spectator.setInput('selectedItems', [{ key: 'value' }, { key: 'value' }]);
        const selectedItemsCounter = spectator.query('span.mr-3');
        expect(spectator.query('.flex-row-reverse')).toHaveClass('selected');
        expect(selectedItemsCounter).toHaveText('2 selected');
    });

    it('should show action-button', () => {
        const options = {
            primary: {
                command: () => {
                    //
                },
                model: [
                    {
                        command: () => {
                            //
                        },
                        icon: 'Test',
                        label: 'Test'
                    }
                ]
            }
        };
        spectator.setInput('options', options );
        expect(spectator.query('dot-action-button')).toExist();
    });

    it('should trigger the methods in the action buttons', () => {
        const primarySpy = jest.fn();
        const secondarySpy = jest.fn();
        const options = {
            secondary: [
                {
                    label: 'Group Actions 1',
                    model: [
                        {
                            command: primarySpy,
                            icon: 'fa fa-refresh',
                            label: 'Action 1-1'
                        }
                    ]
                },
                {
                    label: 'Group Actions 2',
                    model: [
                        {
                            command: secondarySpy,
                            icon: 'fa fa-refresh',
                            label: 'Action 2-1'
                        }
                    ]
                }
            ]
        };
        spectator.setInput('options', options);
        spectator.setInput('selectedItems', [{ key: 'value' }]);

        const splitButtons = spectator.queryAll(SplitButton);
        expect(splitButtons.length).toBe(2);
        expect(splitButtons[0].model).toEqual(options.secondary[0].model);
        expect(splitButtons[1].model).toEqual(options.secondary[1].model);
    });

    it('should not break when when no primary action is passed', () => {
        const options = {
            primary: {
                model: [
                    {
                        command: () => {
                            //
                        },
                        icon: 'Test',
                        label: 'Test'
                    }
                ]
            }
        };
        spectator.setInput('options', options);

        expect(() => {
            spectator.component.handlePrimaryAction();
        }).not.toThrow();
    });
});
