import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { RouterTestingModule } from '@angular/router/testing';

import { Chip, ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentSidebarInformationComponent } from './dot-edit-content-sidebar-information.component';

import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';
import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';

const messageServiceMock = new MockDotMessageService({
    'edit.content.sidebar.information.references-with.pages.not.used': 'No References'
});

describe('DotEditContentSidebarInformationComponent', () => {
    let spectator: Spectator<DotEditContentSidebarInformationComponent>;

    const mockContentlet = {
        inode: '123',
        ownerUserName: 'admin@dotcms.com',
        creationDate: new Date('2024-03-20'),
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
            DotNameFormatPipe,
            ContentletStatusPipe,
            DotMessagePipe
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotFormatDateService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
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
            const chip = spectator.query(Chip);
            expect(chip).toBeTruthy();
        });

        it('should show json link', () => {
            const jsonLink = spectator.query(byTestId('json-link'));
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
        });

        it('should show modified information', () => {
            const modifiedDate = spectator.query(byTestId('modified-date'));
            expect(modifiedDate).toBeTruthy();
        });

        it('should show published information', () => {
            const publishedDate = spectator.query(byTestId('published-date'));
            expect(publishedDate).toBeTruthy();
        });

        it('should show references count', () => {
            const referencesCount = spectator.query(byTestId('references-count'));
            expect(referencesCount).toBeTruthy();
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
            const chip = spectator.query(Chip);
            expect(chip).toBeTruthy();
        });

        it('should not show json link', () => {
            const jsonLink = spectator.query(byTestId('json-link'));
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
