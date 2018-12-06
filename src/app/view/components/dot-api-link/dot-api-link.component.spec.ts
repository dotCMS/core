import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotApiLinkComponent } from './dot-api-link.component';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';

@Component({
    template: `<dot-api-link [href]="href"></dot-api-link>`
})
class TestHostComponent {
    href = 'api/v1/123';

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

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotApiLinkComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        hostDe = hostFixture.debugElement;
        hostComp = hostDe.componentInstance;

        de = hostDe.query(By.css('dot-api-link'));

        hostFixture.detectChanges();
        link = de.query(By.css('a'));

    });

    it('should show label', () => {
        expect(link.nativeElement.textContent).toBe('API');
    });

    it('should set link properties and attr correctly', () => {
        expect(link.attributes).toEqual({
            target: '_blank'
        });
        expect(link.properties).toEqual({
            href: '/api/v1/123',
            title: '/api/v1/123'
        });
    });

    it('should update link when href is change', () => {
        expect(link.properties).toEqual({
            href: '/api/v1/123',
            title: '/api/v1/123'
        });

        hostComp.updateLink('/api/new/1000');
        hostFixture.detectChanges();

        expect(link.properties).toEqual({
            href: '/api/new/1000',
            title: '/api/new/1000'
        });
    });

    it('should set the link relative always', () => {
        hostComp.updateLink('api/no/start/slash');
        hostFixture.detectChanges();

        expect(link.properties).toEqual({
            href: '/api/no/start/slash',
            title: '/api/no/start/slash'
        });
    });
});
