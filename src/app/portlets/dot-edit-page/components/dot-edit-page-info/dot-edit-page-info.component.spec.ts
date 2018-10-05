import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DOTTestBed } from '../../../../test/dot-test-bed';

import { DotEditPageInfoComponent } from './dot-edit-page-info.component';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { mockUser } from '../../../../test/login-service.mock';
import { DotRenderedPageState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { mockDotRenderedPage } from '../../../../test/dot-rendered-page.mock';
import { DotClipboardUtil } from '../../../../api/util/clipboard/ClipboardUtil';
import { SiteServiceMock } from '../../../../test/site-service.mock';
import { SiteService } from 'dotcms-js/dotcms-js';

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.pageurl.copied.clipboard': 'Copied to clipboard',
    'dot.common.message.pageurl.copied.clipboard.error': 'Can not copy to cliploard',
    'editpage.toolbar.page.cant.edit': 'No permissions...',
    'editpage.toolbar.page.locked.by.user': 'Page is locked by...'
});

describe('DotEditPageInfoComponent', () => {
    let component: DotEditPageInfoComponent;
    let fixture: ComponentFixture<DotEditPageInfoComponent>;
    let de: DebugElement;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotClipboardUtil: DotClipboardUtil;
    let siteService: SiteService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditPageInfoComponent],
            providers: [
                DotClipboardUtil,
                DotGlobalMessageService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: SiteService, useClass: SiteServiceMock }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditPageInfoComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        dotClipboardUtil = de.injector.get(DotClipboardUtil);
        siteService = de.injector.get(SiteService);
    });

    describe('default', () => {
        beforeEach(() => {
            spyOnProperty(siteService, 'currentSite', 'get').and.returnValue({
                name: 'demo.dotcms.com'
            });
            component.pageState = new DotRenderedPageState(
                mockUser,
                JSON.parse(JSON.stringify(mockDotRenderedPage))
            );
            fixture.detectChanges();
        });

        it('should set page title', () => {
            const pageTitleEl: HTMLElement = de.query(By.css('.page-info__title')).nativeElement;
            expect(pageTitleEl.textContent).toContain('A title');
        });

        it(
            'should copy to clipboard url',
            fakeAsync(() => {
                spyOn(dotGlobalMessageService, 'display');
                spyOn(dotClipboardUtil, 'copy').and.callFake(() => {
                    return new Promise((resolve) => {
                        resolve(true);
                    });
                });

                const copyUrlButton: DebugElement = de.query(By.css('.page-info__copy-url'));

                copyUrlButton.nativeElement.click();

                tick();
                expect(dotClipboardUtil.copy).toHaveBeenCalledWith(
                    'http://demo.dotcms.com:9876/an/url/test'
                );
                expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Copied to clipboard');
            })
        );

        it(
            'should habdle error of copy to clipboard',
            fakeAsync(() => {
                spyOn(dotGlobalMessageService, 'error');
                spyOn(dotClipboardUtil, 'copy').and.callFake(() => {
                    return new Promise((_resolve, reject) => {
                        reject(true);
                    });
                });

                const copyUrlButton: DebugElement = de.query(By.css('.page-info__copy-url'));

                copyUrlButton.nativeElement.click();

                tick();
                expect(dotGlobalMessageService.error).toHaveBeenCalledWith(
                    'Can not copy to cliploard'
                );
            })
        );
    });
});
