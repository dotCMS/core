import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotActionMenuItem } from '@dotcms/dotcms-models';
import { DotActionMenuButtonComponent, DotMenuComponent } from '@dotcms/ui';
import { dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

describe('ActionMenuButtonComponent', () => {
    let comp: DotActionMenuButtonComponent;
    let fixture: ComponentFixture<DotActionMenuButtonComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                ButtonModule,
                TooltipModule,
                DotMenuComponent,
                BrowserAnimationsModule,
                DotActionMenuButtonComponent
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotActionMenuButtonComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
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

        comp.actions = fakeActions;
        fixture.detectChanges();

        const button = de.query(By.css('.dot-menu__button'));
        const buttonTooltip = de.query(By.css('[data-testid="dot-action-tooltip-button"]'));
        const menu = de.query(By.css('dot-menu'));

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

        comp.actions = fakeActions;
        fixture.detectChanges();

        const actionButtonTooltip = de.query(By.css('[data-testid="dot-action-tooltip-button"]'));
        const actionButtonMenu = de.query(By.css('dot-menu'));

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

        comp.actions = fakeActions;
        comp.item = mockContentType;
        fixture.detectChanges();

        spyOn(fakeActions[0].menuItem, 'command');

        const actionButtonTooltip = de.query(By.css('[data-testid="dot-action-tooltip-button"]'));
        actionButtonTooltip.nativeElement.click();

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

        comp.actions = fakeActions;
        comp.ngOnInit();

        expect(comp.filteredActions.length).toEqual(1);
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

        comp.actions = fakeActions;
        comp.item = mockContentType;
        fixture.detectChanges();

        expect(de.query(By.css('[data-testid="dot-action-tooltip-button"]')) === null).toEqual(
            true,
            'tooltip button hide'
        );
        expect(de.query(By.css('dot-menu')) === null).toEqual(false, 'menu options show');
        expect(de.query(By.css('button')) === null).toEqual(false, 'button to show/hide menu show');
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
        comp.actions = fakeActions;
        comp.item = mockContentType;
        fixture.detectChanges();
        const actionButtonMenu = de.query(By.css('[data-testid="dot-menu-button"]'));
        actionButtonMenu.triggerEventHandler('click', {
            stopPropagation: () => {
                //
            }
        });
        fixture.detectChanges();

        const menuItemsLink = de.queryAll(By.css('.p-menuitem-link'));
        menuItemsLink[1].nativeElement.click();

        expect(fakeCommand).toHaveBeenCalledTimes(1);
        expect(fakeCommand).toHaveBeenCalledWith(mockContentType);
    });
});
