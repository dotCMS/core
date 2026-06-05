import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { RouterTestingModule } from '@angular/router/testing';

import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentSidebarInformationComponent } from './dot-edit-content-sidebar-information.component';

import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';

const messageServiceMock = new MockDotMessageService({
    New: 'New',
    Published: 'Published'
});

describe('DotEditContentSidebarInformationComponent', () => {
    let spectator: Spectator<DotEditContentSidebarInformationComponent>;

    const mockContentlet = {
        inode: '123',
        identifier: 'id-123',
        hasLiveVersion: true,
        live: true,
        working: false,
        archived: false,
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
            SkeletonModule,
            TooltipModule,
            DotNameFormatPipe,
            DotRelativeDatePipe,
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
        jest.clearAllMocks();
        spectator = createComponent({ detectChanges: false });
    });

    describe('with existing contentlet', () => {
        beforeEach(() => {
            spectator.setInput('data', {
                contentlet: mockContentlet,
                contentType: mockContentType,
                referencesPageCount: '5',
                loading: false
            });
            spectator.detectChanges();
        });

        it('should NOT show contentlet status chip', () => {
            expect(spectator.query('dot-contentlet-status-chip')).toBeFalsy();
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

        it('should NOT show a references card', () => {
            expect(spectator.query(byTestId('references-card'))).toBeFalsy();
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

        it('should not show json link', () => {
            const jsonLink = spectator.query(byTestId('json-link'));
            expect(jsonLink).toBeFalsy();
        });

        it('should show content type information', () => {
            const contentTypeLink = spectator.query(byTestId('content-type-link'));
            expect(contentTypeLink).toBeTruthy();
            expect(contentTypeLink.textContent).toContain('Blog');
        });
    });
});
