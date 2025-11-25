import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotLinkComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotApiLinkComponent } from './dot-api-link.component';

@Component({
    template: `
        <dot-api-link [href]="href"></dot-api-link>
    `
})
class TestHostComponent {
    href = 'api/v1/123';
}

describe('DotApiLinkComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostDe: DebugElement;
    let de: DebugElement;
    let link: DebugElement;

    const messageServiceMock = new MockDotMessageService({});

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
            imports: [DotLinkComponent, DotApiLinkComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        hostDe = hostFixture.debugElement;

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
