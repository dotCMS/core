import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DOTTestBed } from '../../../../../../../test/dot-test-bed';

import { DotEditPageLockInfoComponent } from './dot-edit-page-lock-info.component';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../../../../../test/dot-message-service.mock';
import { mockUser } from '../../../../../../../test/login-service.mock';
import { DotRenderedPageState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { mockDotRenderedPage } from '../../../../../../../test/dot-rendered-page.mock';
import { DotClipboardUtil } from '../../../../../../../api/util/clipboard/ClipboardUtil';

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.pageurl.copied.clipboard': 'Copied to clipboard',
    'dot.common.message.pageurl.copied.clipboard.error': 'Can not copy to cliploard',
    'editpage.toolbar.page.cant.edit': 'No permissions...',
    'editpage.toolbar.page.locked.by.user': 'Page is locked by...'
});

describe('DotEditPageLockInfoComponent', () => {
    let component: DotEditPageLockInfoComponent;
    let fixture: ComponentFixture<DotEditPageLockInfoComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditPageLockInfoComponent],
            providers: [
                DotClipboardUtil,
                DotGlobalMessageService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditPageLockInfoComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    describe('default', () => {
        beforeEach(() => {
            component.pageState = new DotRenderedPageState(
                mockUser,
                JSON.parse(
                    JSON.stringify({
                        ...mockDotRenderedPage,
                        page: {
                            ...mockDotRenderedPage.page,
                            canEdit: true,
                            lockedBy: '123'
                        }
                    })
                )
            );
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
                component.pageState = new DotRenderedPageState(
                    mockUser,
                    JSON.parse(
                        JSON.stringify({
                            ...mockDotRenderedPage,
                            page: {
                                ...mockDotRenderedPage.page,
                                canEdit: true,
                                lockedBy: 'another-user'
                            }
                        })
                    )
                );
                fixture.detectChanges();
                lockedMessage = de.query(By.css('.page-info__locked-by-message'));
            });

            it('should have message', () => {
                expect(lockedMessage.nativeElement.textContent.trim()).toEqual(
                    'Page is locked by...'
                );
            });

            it(
                'should blink',
                fakeAsync(() => {
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
                })
            );
        });

        describe('permissions', () => {
            beforeEach(() => {
                component.pageState = new DotRenderedPageState(
                    mockUser,
                    JSON.parse(
                        JSON.stringify({
                            ...mockDotRenderedPage,
                            page: {
                                ...mockDotRenderedPage.page,
                                canEdit: false
                            }
                        })
                    )
                );
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
