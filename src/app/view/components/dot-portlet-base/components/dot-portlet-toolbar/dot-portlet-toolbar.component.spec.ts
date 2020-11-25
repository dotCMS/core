import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotPortletToolbarComponent } from './dot-portlet-toolbar.component';

describe('DotPortletToolbarComponent', () => {
    let component: DotPortletToolbarComponent;
    let fixture: ComponentFixture<DotPortletToolbarComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        cancel: 'Cancel',
                        actions: 'Actions'
                    })
                }
            ],
            declarations: [DotPortletToolbarComponent],
            imports: [ToolbarModule, DotMessagePipeModule, ButtonModule, MenuModule]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPortletToolbarComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
    });

    describe('markup', () => {
        describe('empty', () => {
            it('should have not title', () => {
                fixture.detectChanges();

                const title = de.query(By.css('[data-testId="title"]'));
                expect(title).toBeNull();
            });

            it('should have left and right zone', () => {
                fixture.detectChanges();

                const left = de.query(By.css('[data-testId="leftGroup"]'));
                const right = de.query(By.css('[data-testId="rightGroup"]'));
                const leftExtra = de.query(By.css('[data-testId="leftExtra"]'));
                const rightExtra = de.query(By.css('[data-testId="rightExtra"]'));
                expect(left).not.toBeNull();
                expect(right).not.toBeNull();
                expect(leftExtra).not.toBeNull();
                expect(rightExtra).not.toBeNull();
                expect(leftExtra.nativeElement.childElementCount).toBe(0);
                expect(rightExtra.nativeElement.childElementCount).toBe(0);
            });

            it('should have actions', () => {
                fixture.detectChanges();

                const actionsWrapper = de.query(By.css('[data-testId="actionsWrapper"]'));
                const actionsMenu = de.query(By.css('[data-testId="actionsMenu"]'));
                const actionsCancelButton = de.query(By.css('[data-testId="actionsCancelButton"]'));
                const actionsMenuButton = de.query(By.css('[data-testId="actionsMenuButton"]'));
                const actionsPrimaryButton = de.query(
                    By.css('[data-testId="actionsPrimaryButton"]')
                );

                expect(actionsWrapper).not.toBeNull();
                expect(actionsMenu).toBeNull();
                expect(actionsCancelButton).toBeNull();
                expect(actionsMenuButton).toBeNull();
                expect(actionsPrimaryButton).toBeNull();
            });
        });
    });

    describe('action buttons', () => {
        describe('primary', () => {
            it('should show one button and call function on click', () => {
                const spy = jasmine.createSpy();
                component.actions = {
                    primary: [
                        {
                            label: 'Save',
                            command: spy
                        }
                    ],
                    cancel: () => {}
                };

                fixture.detectChanges();

                const actionsPrimaryButton = de.query(
                    By.css('[data-testId="actionsPrimaryButton"]')
                );
                actionsPrimaryButton.triggerEventHandler('click', {});

                expect(actionsPrimaryButton.nativeElement.textContent).toBe('Save');
                expect(spy).toHaveBeenCalledWith({});

                const actionsMenu = de.query(By.css('[data-testId="actionsMenu"]'));
                expect(actionsMenu).toBeNull();
            });

            it('should bind input actionsMenuButton', () => {
                component.actionsButtonLabel = 'Custom Action Label';
                component.actions = {
                    primary: [
                        {
                            label: 'Design',
                            command: () => {}
                        },
                        {
                            label: 'Code',
                            command: () => {}
                        }
                    ],
                    cancel: () => {}
                };

                fixture.detectChanges();
                const actionsMenuButton = de.query(By.css('[data-testId="actionsMenuButton"]'));
                expect(actionsMenuButton.nativeElement.textContent).toBe('Custom Action Label');
            });

            it('should one button show and handle error', () => {
                spyOn(console, 'error');

                component.actions = {
                    primary: [
                        {
                            label: 'Save',
                            command: () => {
                                throw new Error('');
                            }
                        }
                    ],
                    cancel: () => {}
                };

                fixture.detectChanges();
                const actionsPrimaryButton = de.query(
                    By.css('[data-testId="actionsPrimaryButton"]')
                );

                actionsPrimaryButton.triggerEventHandler('click', {});
                expect(console.error).toHaveBeenCalledTimes(1);
            });

            it('should show dropdown menu', () => {
                component.actions = {
                    primary: [
                        {
                            label: 'Design',
                            command: () => {}
                        },
                        {
                            label: 'Code',
                            command: () => {}
                        }
                    ],
                    cancel: () => {}
                };

                fixture.detectChanges();

                const actionsMenuButton = de.query(By.css('[data-testId="actionsMenuButton"]'));

                expect(actionsMenuButton.nativeElement.textContent).toBe('Actions');
                expect(actionsMenuButton.attributes.icon).toBe('pi pi-chevron-down');
                expect(actionsMenuButton.attributes.iconPos).toBe('right');

                const actionsMenu = de.query(By.css('[data-testId="actionsMenu"]'));
                expect(actionsMenu.componentInstance.model).toEqual([
                    {
                        label: 'Design',
                        command: jasmine.any(Function)
                    },
                    {
                        label: 'Code',
                        command: jasmine.any(Function)
                    }
                ]);
            });
        });

        describe('cancel', () => {
            it('should show and call function on click', () => {
                const spy = jasmine.createSpy();
                component.actions = {
                    primary: null,
                    cancel: spy
                };

                fixture.detectChanges();
                const actionsCancelButton = de.query(By.css('[data-testId="actionsCancelButton"]'));
                actionsCancelButton.triggerEventHandler('click', {});

                expect(actionsCancelButton.classes['p-button-secondary']).toBe(true);
                expect(actionsCancelButton.nativeElement.textContent).toBe('Cancel');
                expect(spy).toHaveBeenCalledWith({});
            });

            it('should bind input cancelButtonLabel', () => {
                component.cancelButtonLabel = 'Custom Cancel Label';
                component.actions = {
                    primary: null,
                    cancel: () => {}
                };

                fixture.detectChanges();
                const actionsCancelButton = de.query(By.css('[data-testId="actionsCancelButton"]'));
                expect(actionsCancelButton.nativeElement.textContent).toBe('Custom Cancel Label');
            });

            it('should show and handle error', () => {
                spyOn(console, 'error');

                component.actions = {
                    primary: null,
                    cancel: () => {
                        throw new Error('');
                    }
                };

                fixture.detectChanges();
                const actionsCancelButton = de.query(By.css('[data-testId="actionsCancelButton"]'));

                actionsCancelButton.triggerEventHandler('click', {});
                expect(console.error).toHaveBeenCalledTimes(1);
            });
        });
    });
});
