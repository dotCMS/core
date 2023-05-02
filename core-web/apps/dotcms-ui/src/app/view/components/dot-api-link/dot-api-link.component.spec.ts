import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotLinkModule } from '@components/dot-link/dot-link.module';

import { DotApiLinkComponent } from './dot-api-link.component';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessageService } from '@dotcms/data-access';

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

    const messageServiceMock = new MockDotMessageService({});

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotApiLinkComponent],
            imports: [DotLinkModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
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
        expect(link.componentInstance.label).toBe('API');
    });

    it('should has the right href', () => {
        expect(link.componentInstance.link).toBe('/api/v1/123');
    });

    it('should has the right icon', () => {
        expect(link.componentInstance.classNames).toBe('pi pi-link');
    });
});
