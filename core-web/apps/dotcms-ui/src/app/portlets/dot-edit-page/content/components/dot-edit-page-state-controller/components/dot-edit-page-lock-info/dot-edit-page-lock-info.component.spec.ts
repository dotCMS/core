import { DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotPageRenderState } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService, mockDotRenderedPage, mockUser } from '@dotcms/utils-testing';

import { DotEditPageLockInfoComponent } from './dot-edit-page-lock-info.component';

const messageServiceMock = new MockDotMessageService({
    'editpage.toolbar.page.cant.edit': 'No permissions...',
    'editpage.toolbar.page.locked.by.user': 'Page is locked by...'
});

describe('DotEditPageLockInfoComponent', () => {
    let component: DotEditPageLockInfoComponent;
    let fixture: ComponentFixture<DotEditPageLockInfoComponent>;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [DotSafeHtmlPipe, DotMessagePipe],
            declarations: [DotEditPageLockInfoComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageLockInfoComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    describe('default', () => {
        beforeEach(() => {
            component.pageState = new DotPageRenderState(mockUser(), {
                ...mockDotRenderedPage(),
                page: {
                    ...mockDotRenderedPage().page,
                    canEdit: true,
                    lockedBy: '123'
                }
            });
            fixture.detectChanges();
        });

        it('should not have error messages', () => {
            const lockedByMessage: DebugElement = de.query(By.css('.page-info__locked-by-message'));
            const cantEditMessage: DebugElement = de.query(By.css('.page-info__cant-edit-message'));

            expect(lockedByMessage === null && cantEditMessage === null).toBe(true);
        });
    });

    describe('locked messages', () => {
        describe('locked by another user', () => {
            let lockedMessage: DebugElement;

            beforeEach(() => {
                component.pageState = new DotPageRenderState(mockUser(), {
                    ...mockDotRenderedPage(),
                    page: {
                        ...mockDotRenderedPage().page,
                        canEdit: true,
                        lockedBy: 'another-user'
                    }
                });
                fixture.detectChanges();
                lockedMessage = de.query(By.css('.page-info__locked-by-message'));
            });

            it('should have message', () => {
                expect(lockedMessage.nativeElement.textContent.trim()).toEqual(
                    messageServiceMock.get('editpage.toolbar.page.locked.by.user')
                );
            });

            it('should blink', fakeAsync(() => {
                spyOn(lockedMessage.nativeElement.classList, 'add');
                spyOn(lockedMessage.nativeElement.classList, 'remove');
                component.blinkLockMessage();

                expect(lockedMessage.nativeElement.classList.add).toHaveBeenCalledWith(
                    'page-info__locked-by-message--blink'
                );
                tick(500);
                expect(lockedMessage.nativeElement.classList.remove).toHaveBeenCalledWith(
                    'page-info__locked-by-message--blink'
                );
            }));
        });

        describe('permissions', () => {
            beforeEach(() => {
                component.pageState = new DotPageRenderState(mockUser(), {
                    ...mockDotRenderedPage(),
                    page: {
                        ...mockDotRenderedPage().page,
                        canEdit: false
                    }
                });
                fixture.detectChanges();
            });

            it("should have don't have permissions messages", () => {
                const lockedMessage: DebugElement = de.query(
                    By.css('.page-info__cant-edit-message')
                );
                expect(lockedMessage.nativeElement.textContent.trim()).toEqual('No permissions...');
            });
        });
    });
});
