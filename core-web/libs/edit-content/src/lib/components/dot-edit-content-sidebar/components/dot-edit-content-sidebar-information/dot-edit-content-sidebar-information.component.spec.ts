import { RouterTestingModule } from '@angular/router/testing';
import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockPipe } from 'ng-mocks';
import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';
import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';
import { DotEditContentSidebarInformationComponent } from './dot-edit-content-sidebar-information.component';

describe('DotEditContentSidebarInformationComponent', () => {
    let spectator: Spectator<DotEditContentSidebarInformationComponent>;

    const mockContentlet = {
        inode: '123',
        ownerName: 'admin@dotcms.com',
        createDate: new Date('2024-03-20'),
        modDate: new Date('2024-03-21'),
        modUserName: 'editor@dotcms.com',
        publishDate: new Date('2024-03-22'),
        publishUserName: 'publisher@dotcms.com'
    };

    const mockContentType = {
        variable: 'blog',
        name: 'Blog'
    };

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarInformationComponent,
        imports: [
            RouterTestingModule,
            ChipModule,
            SkeletonModule,
            TooltipModule,
            MockPipe(DotMessagePipe, (value) => value),
            MockPipe(DotRelativeDatePipe, (value) => value),
            MockPipe(DotNameFormatPipe, (value) => value),
            MockPipe(ContentletStatusPipe, () => ({ label: 'Draft', classes: 'status-draft' }))
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('with existing contentlet', () => {
        beforeEach(() => {
            spectator.setInput('data', {
                contentlet: mockContentlet,
                contentType: mockContentType,
                referencesPageCount: 5,
                loading: false
            });
            spectator.detectChanges();
        });

        it('should show status chip', () => {
            const chip = spectator.query('p-chip');
            expect(chip).toBeTruthy();
            expect(chip.getAttribute('label')).toBe('Draft');
            expect(chip.getAttribute('styleClass')).toContain('status-draft');
        });

        it('should show json link', () => {
            const jsonLink = spectator.query('a[target="_blank"]');
            expect(jsonLink).toBeTruthy();
        });

        it('should show content type information', () => {
            const contentTypeLink = spectator.query(byTestId('content-type-link'));
            expect(contentTypeLink).toBeTruthy();
            expect(contentTypeLink.textContent).toContain('Blog');
        });

        it('should show created information', () => {
            const createdDate = spectator.query(byTestId('created-date'));
            expect(createdDate).toBeTruthy();
            expect(createdDate.textContent.trim()).toBe('2024-03-20');
        });

        it('should show modified information', () => {
            const modifiedDate = spectator.query(byTestId('modified-date'));
            expect(modifiedDate).toBeTruthy();
            expect(modifiedDate.textContent.trim()).toBe('2024-03-21');
        });

        it('should show published information', () => {
            const publishedDate = spectator.query(byTestId('published-date'));
            expect(publishedDate).toBeTruthy();
            expect(publishedDate.textContent.trim()).toBe('2024-03-22');
        });

        it('should show references count', () => {
            const referencesCount = spectator.query(byTestId('references-count'));
            expect(referencesCount).toBeTruthy();
            expect(referencesCount.textContent).toContain(
                'edit.content.sidebar.information.references-with.pages.tooltip'
            );
        });
    });

    describe('with new contentlet', () => {
        beforeEach(() => {
            spectator.setInput('data', {
                contentlet: null,
                contentType: mockContentType,
                referencesPageCount: 0,
                loading: false
            });
            spectator.detectChanges();
        });

        it('should not show status chip', () => {
            const chip = spectator.query('p-chip');
            expect(chip).toBeFalsy();
        });

        it('should not show json link', () => {
            const jsonLink = spectator.query('a[target="_blank"]');
            expect(jsonLink).toBeFalsy();
        });

        it('should show content type information', () => {
            const contentTypeLink = spectator.query(byTestId('content-type-link'));
            expect(contentTypeLink).toBeTruthy();
            expect(contentTypeLink.textContent).toContain('Blog');
        });

        it('should show no references message', () => {
            const referencesCount = spectator.query(byTestId('references-count'));
            expect(referencesCount).toBeTruthy();
            expect(referencesCount.textContent).toContain(
                'edit.content.sidebar.information.references-with.pages.not.used'
            );
        });
    });

    describe('loading state', () => {
        beforeEach(() => {
            spectator.setInput('data', {
                contentlet: null,
                contentType: null,
                referencesPageCount: 0,
                loading: true
            });
            spectator.detectChanges();
        });

        it('should show skeleton loader', () => {
            const skeleton = spectator.query('p-skeleton');
            expect(skeleton).toBeTruthy();
        });
    });
});
