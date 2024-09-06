import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotLinkComponent } from '././dot-link.component';

@Component({
    template: `
        <dot-link [href]="href" [icon]="icon" [label]="label"></dot-link>
    `
})
class TestHostComponent {
    href = 'api/v1/123';
    icon = 'pi-link';
    label = 'dot.common.testing';

    updateLink(href: string): void {
        this.href = href;
    }
}

describe('DotLinkComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: TestHostComponent;
    let de: DebugElement;
    let link: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'dot.common.testing': 'This is a test'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [DotSafeHtmlPipe, DotMessagePipe, DotLinkComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        hostDe = hostFixture.debugElement;
        hostComp = hostDe.componentInstance;

        de = hostDe.query(By.css('dot-link'));
        hostFixture.detectChanges();
        link = de.query(By.css('a'));
    });

    it('should show label', () => {
        expect(link.nativeElement.textContent).toBe('This is a test');
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
