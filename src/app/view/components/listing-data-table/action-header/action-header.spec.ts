import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { Router } from '@angular/router';
import { SplitButtonModule, ButtonModule } from 'primeng/primeng';
import { ActionHeaderComponent } from './action-header';
import { ActionButtonComponent } from '../../_common/action-button/action-button.component';
import { MessageService } from '../../../../api/services/messages-service';
import { ConfirmationService } from 'primeng/components/common/api';
import { MockMessageService } from '../../../../test/message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
class RouterMock {
    navigate(): string {
        return null;
    }
}
describe('ActionHeaderComponent', () => {
    let comp: ActionHeaderComponent;
    let compActionButton: ActionButtonComponent;
    let fixture: ComponentFixture<ActionHeaderComponent>;
    let fixtureActionButton: ComponentFixture<ActionButtonComponent>;
    let de: DebugElement;
    let msjService;
    beforeEach(async(() => {

        let messageServiceMock = new MockMessageService({
            'selected': 'selected'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ActionHeaderComponent, ActionButtonComponent],
            imports: [SplitButtonModule, ButtonModule],
            providers: [
                { provide: Router, useClass: RouterMock },
                { provide: MessageService, useValue: messageServiceMock },
                ConfirmationService
            ]
        });

        fixture = TestBed.createComponent(ActionHeaderComponent);
        fixtureActionButton = TestBed.createComponent(ActionButtonComponent);
        comp = fixture.componentInstance;
        compActionButton = fixtureActionButton.componentInstance;
        de = fixture.debugElement.query(By.css('div'));

        msjService = fixture.debugElement.injector.get(MessageService);
    }));

    it('should render default state correctly', () => {
        let actionButton = de.query(By.css('button'));
        let groupActions = de.query(By.css('.action-header__group-actions'));
        expect(actionButton).not.toBeNull();
        expect(groupActions).toBeNull();
    });

    it('should show the number of items selected', () => {
        let fakeButtons = [
            {
                label: 'Group Actions 1',
                model: [
                    {
                        command: jasmine.createSpy('spy'),
                        icon: 'fa-refresh',
                        label: 'Action 1-1'
                    }
                ]
            }
        ];
        comp.actionButtonItems = fakeButtons;

        let fakeData = [{key: 'value'}, {key: 'value'}];
        let items = 2;

        comp.selectedItems = fakeData;
        comp.selected = true;
        fixture.detectChanges();

        let selectedItemsCounter = de.query(By.css('.action-header__selected-items-counter'));
        expect(selectedItemsCounter.nativeElement.textContent).toBe(items + ' selected');
    });

    it('should pass configuration to action-button options', () => {
        let actionButton = de.query(By.css('.action-header__primary-button'));
        let fakeData = [{
            command: () => {
                this.createContentType('content');
            },
            icon: 'fa-newspaper-o',
            label: 'Content'
        }];

        compActionButton.options = fakeData;

        fixtureActionButton.detectChanges();

        expect(actionButton).not.toBeNull();
        expect(compActionButton.options.length).not.toBeLessThan(0);
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