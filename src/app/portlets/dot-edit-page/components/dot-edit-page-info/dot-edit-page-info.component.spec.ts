import { async, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DOTTestBed } from '../../../../test/dot-test-bed';

import { DotEditPageInfoComponent } from './dot-edit-page-info.component';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { mockUser } from '../../../../test/login-service.mock';
import { DotRenderedPageState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { mockDotRenderedPage } from '../../../../test/dot-rendered-page.mock';
import { SiteServiceMock } from '../../../../test/site-service.mock';
import { SiteService } from 'dotcms-js';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { LOCATION_TOKEN } from 'src/app/providers';

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
    let siteService: SiteService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditPageInfoComponent],
            imports: [DotApiLinkModule, DotCopyButtonModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: SiteService,
                    useClass: SiteServiceMock
                },
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        protocol: 'http:',
                        port: '9876'
                    }
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditPageInfoComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
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
            const pageTitleEl: HTMLElement = de.query(By.css('h2')).nativeElement;
            expect(pageTitleEl.textContent).toContain('A title');
        });

        it('should have api link', () => {
            const apiLink: DebugElement = de.query(By.css('dot-api-link'));
            expect(apiLink.componentInstance.href).toBe(
                '/api/v1/page/render/an/url/test?language_id=1'
            );
        });

        it('should have copy button', () => {
            const button: DebugElement = de.query(By.css('dot-copy-button '));
            expect(button.componentInstance.copy).toBe('http://demo.dotcms.com:9876/an/url/test');
        });
    });
});
