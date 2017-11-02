import { DotConfirmationService } from './../../../../api/services/dot-confirmation/dot-confirmation.service';
import { ActionButtonModule } from '../../_common/action-button/action-button.module';
import { ActionHeaderComponent } from './action-header';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { SplitButtonModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('ActionHeaderComponent', () => {
    let comp: ActionHeaderComponent;
    let fixture: ComponentFixture<ActionHeaderComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        let messageServiceMock = new MockMessageService({
            'selected': 'selected'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ActionHeaderComponent
            ],
            imports: [
                BrowserAnimationsModule,
                ActionButtonModule,
                RouterTestingModule.withRoutes([{
                    component: ActionHeaderComponent,
                    path: 'test'
                }])
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
                DotConfirmationService
            ]
        });

        fixture = DOTTestBed.createComponent(ActionHeaderComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('.action-header'));
    }));

    it('should render default state correctly', () => {
        let actionButton: DebugElement = de.query(By.css('.action-header__primary-button'));
        let groupActions: DebugElement = de.query(By.css('.action-header__secondary-button'));
        expect(actionButton).toBeNull();
        expect(groupActions).toBeNull();
    });

    it('should show the number of items selected', () => {
        comp.selectedItems = [{key: 'value'}, {key: 'value'}];
        fixture.detectChanges();
        let selectedItemsCounter: DebugElement = de.query(By.css('.action-header__selected-items-counter'));
        expect(de.nativeElement.className).toContain('selected');
        expect(selectedItemsCounter.nativeElement.textContent).toBe('2 selected');
    });

    it('should show action-button', () => {
        let options = {
            primary: {
                command: () => {},
                model: [
                    {
                        command: () => {},
                        icon: 'Test',
                        label: 'Test'
                    }
                ]
            }
        };
        comp.options = options;
        fixture.detectChanges();
        const actionButton = de.query(By.css('.action-header__primary-button'));
        expect(actionButton).not.toBeNull();
    });

    it('should trigger the methods in the action buttons', () => {
        let primarySpy = jasmine.createSpy('spy');
        let secondarySpy = jasmine.createSpy('spy2');
        let options = {
            secondary: [
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
                            command: secondarySpy,
                            icon: 'fa-refresh',
                            label: 'Action 2-1'
                        }
                    ]
                }
            ]
        };
        comp.options = options;
        comp.selectedItems = [{key: 'value'}, {key: 'value'}];
        fixture.detectChanges();

        let splitButtons = de.query(By.all()).nativeElement.querySelectorAll('.ui-menuitem-link');
        let primaryButton = splitButtons[0];
        let secondaryButton = splitButtons[1];

        primaryButton.click();
        secondaryButton.click();

        expect(primarySpy).toHaveBeenCalled();
        expect(secondarySpy).toHaveBeenCalled();
    });
});
