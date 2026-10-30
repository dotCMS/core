import { DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { ActionHeaderComponent } from './action-header.component';

import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotActionButtonModule } from '../../_common/dot-action-button/dot-action-button.module';

xdescribe('ActionHeaderComponent', () => {
    let comp: ActionHeaderComponent;
    let fixture: ComponentFixture<ActionHeaderComponent>;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        const messageServiceMock = new MockDotMessageService({
            selected: 'selected'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ActionHeaderComponent],
            imports: [
                BrowserAnimationsModule,
                DotActionButtonModule,
                RouterTestingModule.withRoutes([
                    {
                        component: ActionHeaderComponent,
                        path: 'test'
                    }
                ])
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotAlertConfirmService
            ]
        });

        fixture = DOTTestBed.createComponent(ActionHeaderComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('.action-header'));
    }));

    it('should render default state correctly', () => {
        const actionButton: DebugElement = de.query(By.css('.action-header__primary-button'));
        const groupActions: DebugElement = de.query(By.css('.action-header__secondary-button'));
        expect(actionButton).toBeNull();
        expect(groupActions).toBeNull();
    });

    it('should show the number of items selected', () => {
        comp.selectedItems = [{ key: 'value' }, { key: 'value' }];
        fixture.detectChanges();
        const selectedItemsCounter: DebugElement = de.query(
            By.css('.action-header__selected-items-counter')
        );
        expect(de.nativeElement.className).toContain('selected');
        expect(selectedItemsCounter.nativeElement.textContent).toBe('2 selected');
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
        comp.options = options;
        fixture.detectChanges();
        const actionButton = de.query(By.css('.action-header__primary-button'));
        expect(actionButton).not.toBeNull();
    });

    it('should trigger the methods in the action buttons', () => {
        const primarySpy = jasmine.createSpy('spy');
        const secondarySpy = jasmine.createSpy('spy2');
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
        comp.options = options;
        comp.selectedItems = [{ key: 'value' }, { key: 'value' }];

        const actionButton: DebugElement = de.query(By.css('.action-header__secondary-button'));
        actionButton.triggerEventHandler('click', {});

        fixture.detectChanges();

        const splitButtons = de.query(By.all()).nativeElement.querySelectorAll('.p-menuitem-link');
        const primaryButton = splitButtons[0];
        const secondaryButton = splitButtons[1];

        primaryButton.click();
        secondaryButton.click();

        expect(primarySpy).toHaveBeenCalled();
        expect(secondarySpy).toHaveBeenCalled();
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
        comp.options = options;
        fixture.detectChanges();

        expect(() => {
            comp.handlePrimaryAction();
        }).not.toThrowError();
    });
});
