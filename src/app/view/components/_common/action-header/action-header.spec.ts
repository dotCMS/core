import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { Router } from '@angular/router';
import { SplitButtonModule, ButtonModule } from 'primeng/primeng';
import { ActionHeaderComponent } from './action-header';
class RouterMock {
    navigate(): string {
        return null;
    }
}
describe('ActionHeaderComponent (inline template)', () => {
    let comp: ActionHeaderComponent;
    let fixture: ComponentFixture<ActionHeaderComponent>;
    let de: DebugElement;
    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [ActionHeaderComponent],
            imports: [SplitButtonModule, ButtonModule],
            providers: [{ provide: Router, useClass: RouterMock }]
        });
    }));

    beforeEach(() => {
      fixture = TestBed.createComponent(ActionHeaderComponent);
      comp = fixture.componentInstance;
      de = fixture.debugElement.query(By.css('div'));
    });

    it('should render default state correctly', () => {
        let actionButton = de.query(By.css('button'));
        let globalSearch  = de.query(By.css('input'));
        let groupActions = de.query(By.css('.action-header__group-actions'));
        expect(actionButton).not.toBeNull();
        expect(globalSearch).not.toBeNull();
        expect(groupActions).not.toBeNull();
    });

    it('should show the number of items selected', () => {
        let fakeData = [{key: 'value'}, {key: 'value'}];
        let items = 2;
        let selectedItemsCounter = de.query(By.css('.action-header__selected-items-counter'));
        comp.selectedItems = fakeData;
        comp.selected = true;
        fixture.detectChanges();
        expect(selectedItemsCounter.nativeElement.textContent).toBe(items + ' Selected');
    });

    it('should trigger the action button method', () => {
        let actionButton = de.query(By.css('button'));
        let btnAction;
        btnAction = jasmine.createSpy('actionBtnCommand');
        comp.primaryCommand = btnAction;
        fixture.detectChanges();
        actionButton.triggerEventHandler('click', null);
        expect(btnAction).toHaveBeenCalled();
    });

    it('should trigger the methods in the action buttons', () => {
        let primarySpy = jasmine.createSpy('spy');
        let secondSpy = jasmine.createSpy('spy2');
        let fakeData = [
            {
                label: 'Group Actions 1',
                model: [
                    {
                        command: primarySpy,
                        icon: 'fa-refresh',
                        label: 'Action 1-1'
                    }
                ]
            },
            {
                label: 'Group Actions 2',
                model: [
                    {
                        command: secondSpy,
                        icon: 'fa-refresh',
                        label: 'Action 2-1'
                    }
                ]
            }
        ];
        comp.actionButtonItems = fakeData;
        comp.selected = true;
        fixture.detectChanges();
        let primaryButton = de.query(By.css('.primaryActions .ui-menuitem-link'));
        let secondButton = de.query(By.css('.secondaryActions .ui-menuitem-link'));

        let primaryButtonEl = primaryButton.nativeElement;
        let secondButtonEl = secondButton.nativeElement;

        primaryButtonEl.click();
        secondButtonEl.click();

        expect(primarySpy).toHaveBeenCalled();
        expect(secondSpy).toHaveBeenCalled();
    });

    it('should trigger one method in the action button', () => {
        let primarySpy = jasmine.createSpy('spy');
        let fakeData = [
            {
                label: 'Group Actions 1',
                model: [
                    {
                        command: primarySpy,
                        icon: 'fa-refresh',
                        label: 'Action 1-1'
                    }
                ]
            }
        ];
        comp.actionButtonItems = fakeData;
        comp.selected = true;
        fixture.detectChanges();
        let primaryButton = de.query(By.css('.primaryActions .ui-menuitem-link'));

        let primaryButtonEl = primaryButton.nativeElement;

        primaryButtonEl.click();

        expect(primarySpy).toHaveBeenCalled();
    });
});