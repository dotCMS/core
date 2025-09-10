import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { dotcmsContentTypeBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotRelationshipTreeComponent } from './dot-relationship-tree.component';

const messageServiceMock = new MockDotMessageService({
    'relationship.query.title': 'Lucene Query'
});

const fakeContentType: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    id: '1234567890',
    name: 'ContentTypeName',
    variable: 'helloVariable',
    baseType: 'testBaseType'
};

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-relationship-tree
            [velocityVar]="velocityVar"
            [contentType]="contentType"
            [isParentField]="isParentField"></dot-relationship-tree>
    `,
    standalone: false
})
class TestHostComponent {
    velocityVar = 'Parent.Children';
    contentType = fakeContentType;
    isParentField = false;
}

describe('DotRelationshipTreeComponent', () => {
    describe('with dot - is Child Field', () => {
        let fixture: ComponentFixture<TestHostComponent>;
        let deHost: DebugElement;
        let de: DebugElement;

        beforeEach(async () => {
            await TestBed.configureTestingModule({
                declarations: [TestHostComponent, DotRelationshipTreeComponent],
                imports: [DotIconModule, DotSafeHtmlPipe, DotMessagePipe, DotCopyButtonComponent],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ]
            }).compileComponents();
        });

        beforeEach(() => {
            fixture = TestBed.createComponent(TestHostComponent);
            deHost = fixture.debugElement;
            de = deHost.query(By.css('dot-relationship-tree'));
            fixture.detectChanges();
        });

        it('should have contentType with dot-icon and its active class', () => {
            const parentText = de.query(By.css('[data-testId="dot-tree-nested-text"]'));
            const icon = parentText.nativeElement.previousSibling;
            expect(icon.classList).toContain('dot-tree--active');
        });

        it('should have the parent icon', () => {
            const parentText = de.query(By.css('[data-testId="dot-tree-upper-text"]'));
            const icon = parentText.nativeElement.previousSibling;
            expect(icon.getAttribute('name')).toBe('face');
        });

        it('should set child correctly', () => {
            const children = de.query(By.css('[data-testId="dot-tree-nested-text"]'));
            const parent = de.query(By.css('[data-testId="dot-tree-upper-text"]'));
            expect(children.nativeElement.innerText).toBe('ContentTypeName');
            expect(parent.nativeElement.innerText).toBe('Parent');
        });
    });

    describe('without dot - is Parent Field', () => {
        let fixture: ComponentFixture<TestHostComponent>;
        let deHost: DebugElement;
        let de: DebugElement;

        beforeEach(async () => {
            await TestBed.configureTestingModule({
                declarations: [TestHostComponent, DotRelationshipTreeComponent],
                imports: [DotIconModule, DotSafeHtmlPipe, DotMessagePipe, DotCopyButtonComponent],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ]
            }).compileComponents();
        });

        beforeEach(() => {
            fixture = TestBed.createComponent(TestHostComponent);
            deHost = fixture.debugElement;
            de = deHost.query(By.css('dot-relationship-tree'));
            deHost.componentInstance.velocityVar = 'Parent';
            deHost.componentInstance.isParentField = true;
            fixture.detectChanges();
        });

        it('should have the child icon', () => {
            const parentText = de.query(By.css('[data-testId="dot-tree-nested-text"]'));
            const icon = parentText.nativeElement.previousSibling;
            expect(icon.getAttribute('name')).toBe('child_care');
        });

        it('should have parent set as current content type', () => {
            const child = de.query(By.css('[data-testId="dot-tree-upper-text"]'));
            const currentContentType = de.query(By.css('[data-testId="dot-tree-nested-text"]'));

            expect(child.nativeElement.innerText).toBe('ContentTypeName');
            expect(currentContentType.nativeElement.innerText).toBe('Parent');
        });

        it('should have dot-icon and its active class', () => {
            const parentText = de.query(By.css('[data-testId="dot-tree-upper-text"]'));
            const icon = parentText.nativeElement.previousSibling;
            expect(icon.classList).toContain('dot-tree--active');
        });
    });
});
