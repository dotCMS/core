import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

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

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [TestHostComponent, DotApiLinkComponent]
            }).compileComponents();
        })
    );

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
        expect(link.attributes.target).toEqual('_blank');
        expect(link.properties.href).toEqual('/api/v1/123');
        expect(link.properties.title).toEqual('/api/v1/123');
    });

    it('should update link when href is change', () => {
        expect(link.properties.href).toEqual('/api/v1/123');
        expect(link.properties.title).toEqual('/api/v1/123');

        hostComp.updateLink('/api/new/1000');
        hostFixture.detectChanges();

        expect(link.properties.href).toEqual('/api/new/1000');
        expect(link.properties.title).toEqual('/api/new/1000');
    });

    it('should set the link relative always', () => {
        hostComp.updateLink('api/no/start/slash');
        hostFixture.detectChanges();

        expect(link.properties.href).toEqual('/api/no/start/slash');
        expect(link.properties.title).toEqual('/api/no/start/slash');
    });
});
