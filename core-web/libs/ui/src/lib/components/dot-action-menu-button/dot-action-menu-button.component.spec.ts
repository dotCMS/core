import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotActionMenuItem } from '@dotcms/dotcms-models';
import { DotActionMenuButtonComponent, DotMenuComponent } from '@dotcms/ui';
import { dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

describe('ActionMenuButtonComponent', () => {
    let spectator: Spectator<DotActionMenuButtonComponent>;

    const createComponent = createComponentFactory({
        component: DotActionMenuButtonComponent,
        imports: [
            ButtonModule,
            TooltipModule,
            DotMenuComponent,
            BrowserAnimationsModule,
            DotActionMenuButtonComponent
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
    });

    it('should display a menu button with multiple actions if actions are more than 1', () => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    command: () => {
                        //
                    },
                    icon: 'fa fa-trash',
                    label: 'Remove'
                },
                shouldShow: () => true
            },
            {
                menuItem: {
                    command: () => {
                        //
                    },
                    icon: 'fa fa-pencil',
                    label: 'Edit'
                }
            }
        ];
        spectator.setInput('actions', fakeActions);
        spectator.detectChanges();

        const button = spectator.query('.dot-menu__button');
        const buttonTooltip = spectator.query(byTestId('dot-action-tooltip-button'));
        const menu = spectator.query('dot-menu');

        expect(buttonTooltip).toBeNull();
        expect(button).toBeDefined();
        expect(menu).toBeDefined();
    });

    it('should display a menu button with 1 action without icon', () => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    command: () => {
                        //
                    },
                    label: 'Remove'
                }
            }
        ];

        spectator.setInput('actions', fakeActions);
        spectator.detectChanges();

        const button = spectator.query('.dot-menu__button');
        const buttonTooltip = spectator.query(byTestId('dot-action-tooltip-button'));
        const menu = spectator.query('dot-menu');

        expect(buttonTooltip).toBeNull();
        expect(button).toBeDefined();
        expect(menu).toBeDefined();
    });

    it('should display an icon button tooltip if actions are equal to 1', () => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    command: () => {
                        //
                    },
                    icon: 'fa fa-trash',
                    label: 'Remove'
                }
            }
        ];

        spectator.setInput('actions', fakeActions);
        spectator.detectChanges();

        const actionButtonTooltip = spectator.query(byTestId('dot-action-tooltip-button'));
        const actionButtonMenu = spectator.query('dot-menu');

        expect(actionButtonTooltip).not.toBeNull();
        expect(actionButtonMenu).toBeNull();
    });

    it('should call menu action with item passed', () => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {
                        //
                    }
                }
            }
        ];
        const mockContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };

        spectator.setInput('actions', fakeActions);
        spectator.setInput('item', mockContentType);
        spectator.detectChanges();

        spyOn(fakeActions[0].menuItem, 'command');

        const actionButtonTooltip = spectator.query(byTestId('dot-action-tooltip-button'));
        spectator.click(actionButtonTooltip);

        expect(fakeActions[0].menuItem.command).toHaveBeenCalledTimes(1);
        expect(fakeActions[0].menuItem.command).toHaveBeenCalledWith(mockContentType);
    });

    it('should filter actions based on shouldShow field', () => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {
                        //
                    }
                },
                shouldShow: () => false
            },
            {
                menuItem: {
                    command: () => {
                        //
                    },
                    icon: 'fa fa-pencil',
                    label: 'Edit'
                },
                shouldShow: () => false
            },
            {
                menuItem: {
                    command: () => {
                        //
                    },
                    icon: 'fa fa-plus',
                    label: 'Add'
                },
                shouldShow: () => true
            }
        ];

        spectator.setInput('actions', fakeActions);
        spectator.component.ngOnInit();

        expect(spectator.component.filteredActions.length).toEqual(1);
    });

    it('should render button with submenu', () => {
        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {
                        //
                    }
                }
            },
            {
                menuItem: {
                    icon: 'fa fa-check',
                    label: 'Test',
                    command: () => {
                        //
                    }
                }
            }
        ];
        const mockContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };

        spectator.setInput('actions', fakeActions);
        spectator.setInput('item', mockContentType);
        spectator.detectChanges();

        const actionButtonTooltip = spectator.query(byTestId('dot-action-tooltip-button'));
        const actionButtonMenu = spectator.query('dot-menu');
        const btn = spectator.query('button');

        expect(actionButtonTooltip).toBeNull();
        expect(actionButtonMenu).toBeTruthy();
        expect(btn).toBeTruthy();
    });

    it('should call menu option actions with item passed', () => {
        const fakeCommand = jasmine.createSpy('fakeCommand');

        const fakeActions: DotActionMenuItem[] = [
            {
                menuItem: {
                    icon: 'fa fa-trash',
                    label: 'Remove',
                    command: () => {
                        //
                    }
                }
            },
            {
                menuItem: {
                    icon: 'fa fa-check',
                    label: 'Test',
                    command: (item) => {
                        fakeCommand(item);
                    }
                }
            }
        ];
        const mockContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };
        spectator.setInput('actions', fakeActions);
        spectator.setInput('item', mockContentType);
        spectator.detectChanges();

        const actionButtonMenu = spectator.query(byTestId('dot-menu-button'));
        spectator.click(actionButtonMenu);
        spectator.detectChanges();

        const menuItemsLink = spectator.queryAll('.p-menuitem-link');
        spectator.click(menuItemsLink[1]);

        expect(fakeCommand).toHaveBeenCalledTimes(1);
        expect(fakeCommand).toHaveBeenCalledWith(mockContentType);
    });
});
