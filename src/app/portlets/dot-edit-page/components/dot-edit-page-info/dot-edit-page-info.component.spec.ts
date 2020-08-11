import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { SiteService } from 'dotcms-js';

import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { SiteServiceMock } from '@tests/site-service.mock';
import { mockDotRenderedPage } from '@tests/dot-page-render.mock';
import { mockUser } from '@tests/login-service.mock';

import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotEditPageInfoComponent } from './dot-edit-page-info.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { LOCATION_TOKEN } from 'src/app/providers';
import { DotPipesModule } from '@pipes/dot-pipes.module';

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.pageurl.copy.clipboard': 'Copy url page'
});

describe('DotEditPageInfoComponent', () => {
    let component: DotEditPageInfoComponent;
    let fixture: ComponentFixture<DotEditPageInfoComponent>;
    let de: DebugElement;
    let siteService: SiteService;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                declarations: [DotEditPageInfoComponent],
                imports: [DotApiLinkModule, DotCopyButtonModule, DotPipesModule],
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
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageInfoComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        siteService = de.injector.get(SiteService);
    });

    describe('default', () => {
        beforeEach(() => {
            spyOnProperty(siteService, 'currentSite', 'get').and.returnValue({
                name: 'demo.dotcms.com'
            });
            component.pageState = new DotPageRenderState(
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
            expect(apiLink.componentInstance.link).toBe(
                '/api/v1/page/render/an/url/test?language_id=1'
            );
        });

        it('should have copy button', () => {
            const button: DebugElement = de.query(By.css('dot-copy-button '));
            expect(button.componentInstance.copy).toBe('http://demo.dotcms.com:9876/an/url/test');
            expect(button.componentInstance.tooltipText).toBe('Copy url page');
        });
    });
});
