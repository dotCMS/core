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

    it('should handle action and send command', () => {
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

        const actionButtonTooltip = de.query(By.css('icon-button-tooltip'));
        const spyHandleAction = spyOn(comp, 'handleActionCommand');

        actionButtonTooltip.nativeElement.click();

        expect(spyHandleAction).toHaveBeenCalledWith(mockContentType, new MouseEvent(''));
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
});
