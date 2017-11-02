import { ContentType } from './../../../../portlets/content-types/shared/content-type.model';
import { IconButtonTooltipModule } from './../icon-button-tooltip/icon-button-tooltip.module';
import { ActionMenuButtonComponent } from './action-menu-button.component';
import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { MenuItem, MenuModule } from 'primeng/primeng';
import { DOTTestBed } from '../../../../test/dot-test-bed';

describe('ActionMenuButtonComponent', () => {
    let comp: ActionMenuButtonComponent;
    let fixture: ComponentFixture<ActionMenuButtonComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ActionMenuButtonComponent],
            imports: [
                IconButtonTooltipModule,
                MenuModule
            ]
        });

        fixture = DOTTestBed.createComponent(ActionMenuButtonComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    });

    it('should display a menu button with multiple actions if actions are more than 1', () => {
        const fakeActions: MenuItem[] = [{
            command: () => {},
            icon: 'fa-trash',
            label: 'Remove'
        },
        {
            command: () => {},
            icon: 'fa-edit',
            label: 'Edit'
        }];

        comp.actions = fakeActions;
        fixture.detectChanges();

        const actionButtonTooltip = de.query(By.css('icon-button-tooltip'));
        const actionButtonMenu    = de.query(By.css('p-menu'));
        const uiMenuList = actionButtonMenu.nativeElement.children[0].children[0].children.length;

        expect(actionButtonTooltip).toBeNull();
        expect(uiMenuList).toEqual(fakeActions.length);
    });

    it('should display an icon button tooltip if actions are equal to 1', () => {
        const fakeActions: MenuItem[] = [{
            command: () => {},
            icon: 'fa-trash',
            label: 'Remove'
        }];

        comp.actions = fakeActions;
        fixture.detectChanges();

        const actionButtonTooltip = de.query(By.css('icon-button-tooltip'));
        const actionButtonMenu    = de.query(By.css('p-menu'));

        expect(actionButtonTooltip).not.toBeNull();
        expect(actionButtonMenu).toBeNull();
    });

    it('should handle action and send command', () => {
        const fakeActions: MenuItem[] = [{
            icon: 'fa-trash',
            label: 'Remove',
            command: () => {}
        }];
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

        expect(spyHandleAction).toHaveBeenCalledWith(fakeActions[0], mockContentType, new MouseEvent(''));
    });

});
