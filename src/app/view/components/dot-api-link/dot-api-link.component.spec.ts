import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotApiLinkComponent } from './dot-api-link.component';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { SiteService } from 'dotcms-js';
import { SiteServiceMock, mockSites } from 'src/app/test/site-service.mock';

@Component({
    template: `<dot-api-link [href]="href"></dot-api-link>`
})
class TestHostComponent {
    href = '/api/v1/123';

    updateLink(href: string): void {
        this.href = href;
    }
}

describe('DotApiLinkComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: TestHostComponent;
    let de: DebugElement;
    let link: DebugElement;
    let siteServiceMock: any; // it should be SiteServiceMock but can't cuz extra method in the mock

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotApiLinkComponent],
            imports: [DotIconModule],
            providers: [{
                provide: SiteService,
                useClass: SiteServiceMock
            }]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        hostDe = hostFixture.debugElement;
        hostComp = hostDe.componentInstance;

        de = hostDe.query(By.css('dot-api-link'));

        hostFixture.detectChanges();
        link = de.query(By.css('a'));

        siteServiceMock = hostDe.injector.get(SiteService);
    });

    it('should show label and icon', () => {
        expect(link.nativeElement.textContent).toBe('code API');
        expect(de.query(By.css('dot-icon')).nativeElement.textContent).toBe('code');
    });

    it('should set link properties and attr correctly', () => {
        expect(link.attributes).toEqual({
            target: '_blank'
        });
        expect(link.properties).toEqual({
            href: '//demo.dotcms.com/api/v1/123',
            title: '//demo.dotcms.com/api/v1/123'
        });
    });

    it('should update host link when site change', () => {
        expect(link.properties).toEqual({
            href: '//demo.dotcms.com/api/v1/123',
            title: '//demo.dotcms.com/api/v1/123'
        });

        siteServiceMock.setFakeCurrentSite(mockSites[1]);
        hostFixture.detectChanges();

        expect(link.properties).toEqual({
            href: '//hello.dotcms.com/api/v1/123',
            title: '//hello.dotcms.com/api/v1/123'
        });
    });

    it('should update link when href is change', () => {
        expect(link.properties).toEqual({
            href: '//demo.dotcms.com/api/v1/123',
            title: '//demo.dotcms.com/api/v1/123'
        });

        hostComp.updateLink('/api/new/1000');
        hostFixture.detectChanges();

        expect(link.properties).toEqual({
            href: '//demo.dotcms.com/api/new/1000',
            title: '//demo.dotcms.com/api/new/1000'
        });
    });
});
