import { ContentType } from './../../../../portlets/content-types/shared/content-type.model';
import { IconButtonTooltipModule } from './../icon-button-tooltip/icon-button-tooltip.module';
import { ActionMenuButtonComponent } from './action-menu-button.component';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { MenuModule } from 'primeng/primeng';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotDataTableAction } from '../../../../shared/models/data-table/dot-data-table-action';

describe('ActionMenuButtonComponent', () => {
    let comp: ActionMenuButtonComponent;
    let fixture: ComponentFixture<ActionMenuButtonComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ActionMenuButtonComponent],
            imports: [IconButtonTooltipModule, MenuModule]
        });

        fixture = DOTTestBed.createComponent(ActionMenuButtonComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    });

    it('should display a menu button with multiple actions if actions are more than 1', () => {
        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    command: () => {},
                    icon: 'fa-trash',
                    label: 'Remove'
                },
                shouldShow: () => true
            },
            {
                menuItem: {
                    command: () => {},
                    icon: 'fa-edit',
                    label: 'Edit'
                }
            }
        ];

        comp.actions = fakeActions;
        fixture.detectChanges();

        const button = de.query(By.css('button'));
        const buttonTooltip = de.query(By.css('icon-button-tooltip'));
        const menu = de.query(By.css('p-menu'));

        expect(buttonTooltip).toBeNull();
        expect(button).toBeDefined();
        expect(menu).toBeDefined();
    });

    it('should display an icon button tooltip if actions are equal to 1', () => {
        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    command: () => {},
                    icon: 'fa-trash',
                    label: 'Remove'
                }
            }
        ];

        comp.actions = fakeActions;
        fixture.detectChanges();

        const actionButtonTooltip = de.query(By.css('icon-button-tooltip'));
        const actionButtonMenu = de.query(By.css('p-menu'));

        expect(actionButtonTooltip).not.toBeNull();
        expect(actionButtonMenu).toBeNull();
    });

    it('should call menu action with item passed', () => {
        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    icon: 'fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            }
        ];
        const mockContentType: ContentType = {
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

        const actionButtonTooltip = de.query(By.css('icon-button-tooltip'));
        actionButtonTooltip.nativeElement.click();

        expect(fakeActions[0].menuItem.command).toHaveBeenCalledTimes(1);
        expect(fakeActions[0].menuItem.command).toHaveBeenCalledWith(mockContentType);
    });

    it('should filter actions based on shouldShow field', () => {
        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    icon: 'fa-trash',
                    label: 'Remove',
                    command: () => {}
                },
                shouldShow: () => false
            },
            {
                menuItem: {
                    command: () => {},
                    icon: 'fa-edit',
                    label: 'Edit'
                },
                shouldShow: () => false
            },
            {
                menuItem: {
                    command: () => {},
                    icon: 'fa-add',
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
        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    icon: 'fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            },
            {
                menuItem: {
                    icon: 'fa-check',
                    label: 'Test',
                    command: () => {}
                }
            }
        ];
        const mockContentType: ContentType = {
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

        expect(de.query(By.css('icon-button-tooltip')) === null).toEqual(true, 'tooltip button hide');
        expect(de.query(By.css('p-menu')) === null).toEqual(false, 'menu options show');
        expect(de.query(By.css('button')) === null).toEqual(false, 'button to show/hide menu show');
    });

    it('should call menu option actions with item passed', () => {
        const fakeCommand = jasmine.createSpy('fakeCommand');

        const fakeActions: DotDataTableAction[] = [
            {
                menuItem: {
                    icon: 'fa-trash',
                    label: 'Remove',
                    command: () => {}
                }
            },
            {
                menuItem: {
                    icon: 'fa-check',
                    label: 'Test',
                    command: (item) => {
                        fakeCommand(item);
                    }
                }
            }
        ];
        const mockContentType: ContentType = {
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

        const menuItemsLink = de.queryAll(By.css('.ui-menu-list a'));
        menuItemsLink[1].nativeElement.click();

        expect(fakeCommand).toHaveBeenCalledTimes(1);
        expect(fakeCommand).toHaveBeenCalledWith(mockContentType);
    });
});
