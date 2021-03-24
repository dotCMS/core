import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotCMSContentType } from '@dotcms/dotcms-models';

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

describe('DotRelationshipTreeComponent', () => {
    describe('with dot', () => {
        let component: DotRelationshipTreeComponent;
        let fixture: ComponentFixture<DotRelationshipTreeComponent>;
        let de: DebugElement;

        beforeEach(async () => {
            await TestBed.configureTestingModule({
                declarations: [DotRelationshipTreeComponent],
                imports: [DotIconModule, DotPipesModule, DotCopyButtonModule],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ]
            }).compileComponents();
        });

        beforeEach(() => {
            fixture = TestBed.createComponent(DotRelationshipTreeComponent);
            component = fixture.componentInstance;
            de = fixture.debugElement;
            component.velocityVar = 'Parent.Children';
            component.contentType = fakeContentType;
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

    describe('without dot', () => {
        let component: DotRelationshipTreeComponent;
        let fixture: ComponentFixture<DotRelationshipTreeComponent>;
        let de: DebugElement;

        beforeEach(async () => {
            await TestBed.configureTestingModule({
                declarations: [DotRelationshipTreeComponent],
                imports: [DotIconModule, DotPipesModule, DotCopyButtonModule],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ]
            }).compileComponents();
        });

        beforeEach(() => {
            fixture = TestBed.createComponent(DotRelationshipTreeComponent);
            component = fixture.componentInstance;
            de = fixture.debugElement;
            component.velocityVar = 'Parent';
            component.contentType = fakeContentType;
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
