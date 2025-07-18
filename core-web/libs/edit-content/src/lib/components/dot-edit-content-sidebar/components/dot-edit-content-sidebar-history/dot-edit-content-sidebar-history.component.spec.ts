import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TimelineModule } from 'primeng/timeline';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotEditContentSidebarHistoryComponent } from './dot-edit-content-sidebar-history.component';

describe('DotEditContentSidebarHistoryComponent', () => {
    let component: DotEditContentSidebarHistoryComponent;
    let fixture: ComponentFixture<DotEditContentSidebarHistoryComponent>;

    const mockHistoryItems: DotCMSContentlet[] = [
        {
            inode: 'test-inode-1',
            title: 'Test Content v1',
            modDate: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
            modUser: 'admin',
            modUserName: 'Admin User',
            live: true,
            working: true,
            identifier: 'test-identifier-1',
            contentType: 'testContent',
            archived: false,
            deleted: false,
            locked: false,
            languageId: 1,
            folder: '',
            host: '',
            hasLiveVersion: true,
            hasTitleImage: false,
            hostName: '',
            owner: 'admin',
            sortOrder: 0,
            stInode: '',
            titleImage: '',
            url: '',
            baseType: 'CONTENT'
        } as DotCMSContentlet,
        {
            inode: 'test-inode-2',
            title: 'Test Content v2',
            modDate: new Date(Date.now() - 172800000).toISOString(), // 2 days ago
            modUser: 'editor',
            modUserName: 'Editor User',
            live: false,
            working: true,
            identifier: 'test-identifier-2',
            contentType: 'testContent',
            archived: false,
            deleted: false,
            locked: false,
            languageId: 1,
            folder: '',
            host: '',
            hasLiveVersion: false,
            hasTitleImage: false,
            hostName: '',
            owner: 'editor',
            sortOrder: 0,
            stInode: '',
            titleImage: '',
            url: '',
            baseType: 'CONTENT'
        } as DotCMSContentlet,
        {
            inode: 'test-inode-3',
            title: 'Test Content v3',
            modDate: new Date(Date.now() - 259200000).toISOString(), // 3 days ago
            modUser: 'author',
            modUserName: 'Author User',
            live: true,
            working: false,
            identifier: 'test-identifier-3',
            contentType: 'testContent',
            archived: false,
            deleted: false,
            locked: false,
            languageId: 1,
            folder: '',
            host: '',
            hasLiveVersion: true,
            hasTitleImage: false,
            hostName: '',
            owner: 'author',
            sortOrder: 0,
            stInode: '',
            titleImage: '',
            url: '',
            baseType: 'CONTENT'
        } as DotCMSContentlet
    ];

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                DotEditContentSidebarHistoryComponent,
                TimelineModule,
                AvatarModule,
                ButtonModule,
                MenuModule,
                SkeletonModule,
                DotGravatarDirective,
                DotMessagePipe,
                DotRelativeDatePipe
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentSidebarHistoryComponent);
        component = fixture.componentInstance;
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });

        it('should have default input values', () => {
            expect(component.$historyItems()).toEqual([]);
            expect(component.$status()).toBe(ComponentStatus.LOADING);
            expect(component.$contentIdentifier()).toBe('');
        });

        it('should initialize with empty expanded items', () => {
            expect(component.expandedItems()).toEqual(new Set());
        });
    });

    describe('Loading State', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('status', ComponentStatus.LOADING);
            fixture.detectChanges();
        });

        it('should show loading state when isLoading is true', () => {
            const loadingElement = fixture.debugElement.query(
                By.css('[data-testid="loading-state"]')
            );
            expect(loadingElement).toBeTruthy();
        });

        it('should show skeleton placeholders', () => {
            const skeletonElements = fixture.debugElement.queryAll(By.css('p-skeleton'));
            expect(skeletonElements.length).toBeGreaterThan(0);
        });

        it('should not show history content when loading', () => {
            const historyTimeline = fixture.debugElement.query(
                By.css('[data-testid="history-timeline"]')
            );
            expect(historyTimeline).toBeFalsy();
        });
    });

    describe('Empty State', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('status', ComponentStatus.LOADED);
            fixture.componentRef.setInput('historyItems', []);
            fixture.detectChanges();
        });

        it('should show empty state when no history items', () => {
            const emptyElement = fixture.debugElement.query(By.css('[data-testid="empty-state"]'));
            expect(emptyElement).toBeTruthy();
        });

        it('should show empty state icon', () => {
            const iconElement = fixture.debugElement.query(
                By.css('.history__empty-icon .pi-history')
            );
            expect(iconElement).toBeTruthy();
        });

        it('should not show history timeline when empty', () => {
            const historyTimeline = fixture.debugElement.query(
                By.css('[data-testid="history-timeline"]')
            );
            expect(historyTimeline).toBeFalsy();
        });
    });

    describe('History Content', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('status', ComponentStatus.LOADED);
            fixture.componentRef.setInput('historyItems', mockHistoryItems);
            fixture.detectChanges();
        });

        it('should show history timeline when items are available', () => {
            const historyTimeline = fixture.debugElement.query(
                By.css('[data-testid="history-timeline"]')
            );
            expect(historyTimeline).toBeTruthy();
        });

        it('should display correct number of history items', () => {
            const historyItems = fixture.debugElement.queryAll(
                By.css('[data-testid="history-item"]')
            );
            expect(historyItems.length).toBe(mockHistoryItems.length);
        });

        it('should display time information correctly', () => {
            const timeElements = fixture.debugElement.queryAll(
                By.css('[data-testid="time-display"]')
            );
            expect(timeElements[0].nativeElement.textContent.trim()).toBe('Now');
            // Other elements will show relative dates
            expect(timeElements.length).toBe(mockHistoryItems.length);
        });

        it('should display user names correctly', () => {
            const userElements = fixture.debugElement.queryAll(
                By.css('[data-testid="history-user"]')
            );
            expect(userElements[0].nativeElement.textContent.trim()).toBe('Admin User');
            expect(userElements[1].nativeElement.textContent.trim()).toBe('Editor User');
            expect(userElements[2].nativeElement.textContent.trim()).toBe('Author User');
        });

        it('should display titles correctly', () => {
            const titleElements = fixture.debugElement.queryAll(
                By.css('[data-testid="history-title"]')
            );
            expect(titleElements[0].nativeElement.textContent.trim()).toBe('Test Content v1');
            expect(titleElements[1].nativeElement.textContent.trim()).toBe('Test Content v2');
            expect(titleElements[2].nativeElement.textContent.trim()).toBe('Test Content v3');
        });
    });

    describe('Status Functionality', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('status', ComponentStatus.LOADED);
            fixture.componentRef.setInput('historyItems', mockHistoryItems);
            fixture.detectChanges();
        });

        it('should return correct status label for published content', () => {
            const publishedItem = mockHistoryItems[0]; // live: true, working: true
            expect(component.getStatusLabel(publishedItem)).toBe('Published');
        });

        it('should return correct status label for working content', () => {
            const workingItem = mockHistoryItems[1]; // live: false, working: true
            expect(component.getStatusLabel(workingItem)).toBe('Working');
        });

        it('should return correct status label for live content', () => {
            const liveItem = mockHistoryItems[2]; // live: true, working: false
            expect(component.getStatusLabel(liveItem)).toBe('Live');
        });

        it('should return correct status CSS class for published content', () => {
            const publishedItem = mockHistoryItems[0];
            expect(component.getStatusClass(publishedItem)).toBe('history__status--published');
        });

        it('should return correct status CSS class for working content', () => {
            const workingItem = mockHistoryItems[1];
            expect(component.getStatusClass(workingItem)).toBe('history__status--working');
        });

        it('should return correct status CSS class for live content', () => {
            const liveItem = mockHistoryItems[2];
            expect(component.getStatusClass(liveItem)).toBe('history__status--live');
        });
    });

    describe('Timeline Functionality', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('status', ComponentStatus.LOADED);
            fixture.componentRef.setInput('historyItems', mockHistoryItems);
            fixture.detectChanges();
        });

        it('should have timeline markers for each item', () => {
            const markers = fixture.debugElement.queryAll(
                By.css('[data-testid="timeline-marker"]')
            );
            expect(markers.length).toBe(mockHistoryItems.length);
        });

        it('should display user avatars', () => {
            const avatars = fixture.debugElement.queryAll(By.css('[data-testid="user-avatar"]'));
            expect(avatars.length).toBe(mockHistoryItems.length);
        });

        it('should show version menu buttons', () => {
            const menuButtons = fixture.debugElement.queryAll(
                By.css('[data-testid="version-menu-button"]')
            );
            expect(menuButtons.length).toBe(mockHistoryItems.length);
        });

        it('should toggle details when toggle button is clicked', () => {
            const toggleButton = fixture.debugElement.query(
                By.css('[data-testid="toggle-details"]')
            );

            // Initially details should not be visible
            let detailsElement = fixture.debugElement.query(
                By.css('[data-testid="history-details"]')
            );
            expect(detailsElement).toBeFalsy();

            // Click toggle button
            toggleButton.nativeElement.click();
            fixture.detectChanges();

            // Details should now be visible
            detailsElement = fixture.debugElement.query(By.css('[data-testid="history-details"]'));
            expect(detailsElement).toBeTruthy();
        });

        it('should expand/collapse functionality work correctly', () => {
            expect(component.isExpanded('test-inode-1')).toBe(false);

            component.toggleExpanded('test-inode-1');
            expect(component.isExpanded('test-inode-1')).toBe(true);

            component.toggleExpanded('test-inode-1');
            expect(component.isExpanded('test-inode-1')).toBe(false);
        });
    });

    describe('Computed Properties', () => {
        it('should compute isLoading correctly', () => {
            fixture.componentRef.setInput('status', ComponentStatus.LOADING);
            expect(component.$isLoading()).toBe(true);

            fixture.componentRef.setInput('status', ComponentStatus.LOADED);
            expect(component.$isLoading()).toBe(false);
        });

        it('should compute hasHistoryItems correctly', () => {
            fixture.componentRef.setInput('historyItems', []);
            expect(component.$hasHistoryItems()).toBe(false);

            fixture.componentRef.setInput('historyItems', mockHistoryItems);
            expect(component.$hasHistoryItems()).toBe(true);
        });
    });

    describe('Accessibility', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('status', ComponentStatus.LOADED);
            fixture.componentRef.setInput('historyItems', mockHistoryItems);
            fixture.detectChanges();
        });

        it('should have proper ARIA labels', () => {
            const section = fixture.debugElement.query(By.css('section'));
            expect(section.nativeElement.getAttribute('aria-label')).toBe(
                'Content Version History'
            );
        });

        it('should have proper timeline structure', () => {
            const timeline = fixture.debugElement.query(By.css('[data-testid="history-timeline"]'));
            expect(timeline).toBeTruthy();

            const timelineItems = fixture.debugElement.queryAll(
                By.css('[data-testid="history-item"]')
            );
            expect(timelineItems.length).toBe(mockHistoryItems.length);

            const timelineMarkers = fixture.debugElement.queryAll(
                By.css('[data-testid="timeline-marker"]')
            );
            expect(timelineMarkers.length).toBe(mockHistoryItems.length);
        });
    });
});
