import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotLinkModule } from '@components/dot-link/dot-link.module';

import { DotApiLinkComponent } from './dot-api-link.component';

@Component({
    template: `<dot-api-link [link]="href"></dot-api-link>`
})
class TestHostComponent {
    href = 'api/v1/123';

    updateLink(href: string): void {
        this.href = href;
    }
}

xdescribe('DotApiLinkComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: TestHostComponent;
    let de: DebugElement;
    let link: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotApiLinkComponent],
            imports: [DotLinkModule]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        hostDe = hostFixture.debugElement;
        hostComp = hostDe.componentInstance;

        de = hostDe.query(By.css('dot-api-link'));

        hostFixture.detectChanges();
        link = de.query(By.css('dot-link'));
    });

    it('should show label', () => {
        expect(link.nativeElement.label).toBe('API');
    });

    xit('should set link properties and attr correctly', () => {
        expect(link.attributes.target).toEqual('_blank');
        expect(link.properties.href).toEqual('/api/v1/123');
        expect(link.properties.title).toEqual('/api/v1/123');
    });

    xit('should update link when href is change', () => {
        expect(link.properties.href).toEqual('/api/v1/123');
        expect(link.properties.title).toEqual('/api/v1/123');

        hostComp.updateLink('/api/new/1000');
        hostFixture.detectChanges();

        expect(link.properties.href).toEqual('/api/new/1000');
        expect(link.properties.title).toEqual('/api/new/1000');
    });

    xit('should set the link relative always', () => {
        hostComp.updateLink('api/no/start/slash');
        hostFixture.detectChanges();

        expect(link.properties.href).toEqual('/api/no/start/slash');
        expect(link.properties.title).toEqual('/api/no/start/slash');
    });
});
