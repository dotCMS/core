import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotApiLinkComponent } from './dot-api-link.component';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { SiteService } from 'dotcms-js';
import { SiteServiceMock } from 'src/app/test/site-service.mock';

@Component({
    template: `<dot-api-link href="/api/v1/123"></dot-api-link>`
})
class TestHostComponent {}

describe('DotApiLinkComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostDe: DebugElement;
    let de: DebugElement;
    let link: DebugElement;

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

        de = hostDe.query(By.css('dot-api-link'));

        hostFixture.detectChanges();
        link = de.query(By.css('a'));
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
});
