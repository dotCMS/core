import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotApiLinkComponent } from './dot-api-link.component';

@Component({
    template: `<dot-api-link [href]="href"></dot-api-link>`
})
class TestHostComponent {
    href = 'api/v1/123';

    updateLink(href: string): void {
        this.href = href;
    }
}

fdescribe('DotApiLinkComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: TestHostComponent;
    let de: DebugElement;
    let deComp: DotApiLinkComponent;

    beforeEach(waitForAsync(() => {
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
        deComp = de.componentInstance;
    });

    it('should set link correctly', () => {
        expect(deComp.link).toEqual('/api/v1/123');
    });

    it('should update link when href is change', () => {
        expect(deComp.link).toEqual('/api/v1/123');

        hostComp.updateLink('/api/new/1000');
        hostFixture.detectChanges();

        expect(deComp.link).toEqual('/api/new/1000');
    });

    it('should set the link relative always', () => {
        hostComp.updateLink('api/no/start/slash');
        hostFixture.detectChanges();

        expect(deComp.link).toEqual('/api/no/start/slash');
    });
});
