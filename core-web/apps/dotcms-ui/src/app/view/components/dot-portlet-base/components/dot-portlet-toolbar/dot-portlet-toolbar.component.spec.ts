import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPortletToolbarComponent } from './dot-portlet-toolbar.component';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-portlet-toolbar>
            <div data-testId="leftExtraContent" left></div>
            <div data-testId="rightExtraContent" right></div>
        </dot-portlet-toolbar>
    `,
    standalone: false
})
class TestHostComponent {}

describe('DotPortletToolbarComponent', () => {
    let component: DotPortletToolbarComponent;
    let hostfixture: ComponentFixture<TestHostComponent>;
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
            declarations: [DotPortletToolbarComponent, TestHostComponent],
            imports: [ToolbarModule, DotMessagePipe, ButtonModule, MenuModule]
        }).compileComponents();
    });

    beforeEach(() => {
        hostfixture = TestBed.createComponent(TestHostComponent);
        de = hostfixture.debugElement;
        component = de.query(By.css('dot-portlet-toolbar')).componentInstance;
    });

    describe('markup', () => {
        describe('empty', () => {
            it('should have not title', () => {
                hostfixture.detectChanges();

                const title = de.query(By.css('[data-testId="title"]'));
                expect(title).toBeNull();
            });

            it('should have left and right zone', () => {
                hostfixture.detectChanges();
                const left = de.query(By.css('[data-testId="leftGroup"]'));
                const right = de.query(By.css('[data-testId="rightGroup"]'));
                const leftExtraContent = de.query(By.css('[data-testId="leftExtraContent"]'));
                const rightExtraContent = de.query(By.css('[data-testId="rightExtraContent"]'));
                expect(left).not.toBeNull();
                expect(right).not.toBeNull();
                expect(leftExtraContent).not.toBeNull();
                expect(rightExtraContent).not.toBeNull();
            });

            it('should have actions', () => {
                hostfixture.detectChanges();

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
                    cancel: () => {
                        //
                    }
                };

                hostfixture.detectChanges();

                const actionsPrimaryButton = de.query(
                    By.css('[data-testId="actionsPrimaryButton"]')
                );
                actionsPrimaryButton.triggerEventHandler('click', {});

                expect(actionsPrimaryButton.nativeElement.textContent).toBe('Save');
                expect(spy).toHaveBeenCalled();

                const actionsMenu = de.query(By.css('[data-testId="actionsMenu"]'));
                expect(actionsMenu).toBeNull();
            });

            it('should bind input actionsMenuButton', () => {
                component.actionsButtonLabel = 'Custom Action Label';
                component.actions = {
                    primary: [
                        {
                            label: 'Design',
                            command: () => {
                                //
                            }
                        },
                        {
                            label: 'Code',
                            command: () => {
                                //
                            }
                        }
                    ],
                    cancel: () => {
                        //
                    }
                };

                hostfixture.detectChanges();
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
                    cancel: () => {
                        //
                    }
                };

                hostfixture.detectChanges();
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
                            command: () => {
                                //
                            }
                        },
                        {
                            label: 'Code',
                            command: () => {
                                //
                            }
                        }
                    ],
                    cancel: () => {
                        //
                    }
                };

                hostfixture.detectChanges();

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

                hostfixture.detectChanges();
                const actionsCancelButton = de.query(By.css('[data-testId="actionsCancelButton"]'));
                actionsCancelButton.triggerEventHandler('click', {});

                expect(actionsCancelButton.nativeElement.textContent).toBe('Cancel');
                expect(spy).toHaveBeenCalledWith({});
            });

            it('should bind input cancelButtonLabel', () => {
                component.cancelButtonLabel = 'Custom Cancel Label';
                component.actions = {
                    primary: null,
                    cancel: () => {
                        //
                    }
                };

                hostfixture.detectChanges();
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

                hostfixture.detectChanges();
                const actionsCancelButton = de.query(By.css('[data-testId="actionsCancelButton"]'));

                actionsCancelButton.triggerEventHandler('click', {});
                expect(console.error).toHaveBeenCalledTimes(1);
            });
        });
    });
});
