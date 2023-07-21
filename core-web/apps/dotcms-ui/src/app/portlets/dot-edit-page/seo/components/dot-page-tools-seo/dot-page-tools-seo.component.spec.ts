import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DialogModule } from 'primeng/dialog';

import { DotMessageService, DotPageToolsService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsSeoComponent } from './dot-page-tools-seo.component';

@Component({
    selector: 'dot-test-host-component',
    template: `<dot-page-tools-seo
        [visible]="visible"
        [currentPageUrl]="currentPageUrl"
    ></dot-page-tools-seo>`
})
class TestHostComponent {
    @Input() visible = true;
    @Input() currentPageUrl = 'https://demo.dotcms.com';
}

describe('DotPageToolsSeoComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let component: DotPageToolsSeoComponent;
    let de: DebugElement;
    let deHost: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.page.tools': 'Page Tools'
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                HttpClientTestingModule,
                DotPageToolsSeoComponent,
                DialogModule,
                BrowserAnimationsModule,
                DotMessagePipe
            ],
            providers: [
                DotPageToolsService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: HttpClient,
                    useValue: {
                        get: () => of(mockPageTools),
                        request: () => of(mockPageTools)
                    }
                }
            ],
            declarations: [TestHostComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;

        de = deHost.query(By.css('dot-page-tools-seo'));
        component = de.componentInstance;

        fixtureHost.detectChanges();
    });

    it('should have page tool list', () => {
        const menuListItems: DebugElement[] = fixtureHost.debugElement.queryAll(
            By.css('.page-tools-list__item')
        );

        expect(menuListItems.length).toEqual(3);
    });

    it('should have correct href values in links', () => {
        const tools = mockPageTools.pageTools;

        const anchorElements: DebugElement[] = fixtureHost.debugElement.queryAll(
            By.css('.page-tools-list__link')
        );

        expect(anchorElements.length).toEqual(3);

        anchorElements.forEach((anchorElement, index) => {
            const href = anchorElement.nativeElement.getAttribute('href');
            expect(href).toEqual(component.getRunnableLink(tools[index].runnableLink));
        });
    });
});
