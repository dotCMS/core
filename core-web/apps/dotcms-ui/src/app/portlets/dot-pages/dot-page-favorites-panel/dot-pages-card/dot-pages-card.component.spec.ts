import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { DotPagesFavoritePageEmptySkeletonComponent } from '@dotcms/ui';

import { DotPagesCardComponent } from './dot-pages-card.component';

interface HostComponent {
    navigateToPageEmitted: boolean | null;
    openMenuEmitted: MouseEvent | null;
    onNavigateToPage: (event: boolean) => void;
    onOpenMenu: (event: MouseEvent) => void;
    actionButtonId: string;
    imageUri: string;
    title: string;
    url: string;
}

describe('DotPagesCardComponent', () => {
    let spectator: SpectatorHost<DotPagesCardComponent>;
    const host = () => spectator.hostComponent as HostComponent;

    const createHost = createHostFactory({
        component: DotPagesCardComponent,
        imports: [
            DotPagesCardComponent,
            CardModule,
            ButtonModule,
            DotPagesFavoritePageEmptySkeletonComponent
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dot-pages-card
                [actionButtonId]="actionButtonId"
                [imageUri]="imageUri"
                [title]="title"
                [url]="url"
                (navigateToPage)="onNavigateToPage($event)"
                (openMenu)="onOpenMenu($event)"
            />`,
            {
                hostProps: {
                    actionButtonId: 'action-button-1',
                    imageUri: 'https://example.com/image.jpg',
                    title: 'Test Page Title',
                    url: '/test-page-url',
                    navigateToPageEmitted: null as boolean | null,
                    openMenuEmitted: null as MouseEvent | null,
                    onNavigateToPage(event: boolean) {
                        this.navigateToPageEmitted = event;
                    },
                    onOpenMenu(event: MouseEvent) {
                        this.openMenuEmitted = event;
                    }
                }
            }
        );
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Input Testing', () => {
        it('should display the default title from host component', () => {
            expect(spectator.component.$title()).toBe('Test Page Title');

            const titleElement = spectator.query('.truncate.w-full');
            expect(titleElement?.textContent?.trim()).toBe('Test Page Title');
        });

        it('should display the default url from host component', () => {
            expect(spectator.component.$url()).toBe('/test-page-url');

            const urlElement = spectator.query('.truncate.w-full.text-sm');
            expect(urlElement?.textContent?.trim()).toBe('/test-page-url');
        });

        it('should display the default imageUri from host component', () => {
            expect(spectator.component.$imageUri()).toBe('https://example.com/image.jpg');

            const imgElement = spectator.query('img') as HTMLImageElement;
            expect(imgElement).toBeTruthy();
            expect(imgElement?.src).toContain('image.jpg');
        });

        it('should set the actionButtonId from host component', () => {
            expect(spectator.component.$actionButtonId()).toBe('action-button-1');

            const button = spectator.query('#action-button-1');
            expect(button).toBeTruthy();
        });
    });

    describe('Template Rendering', () => {
        it('should render the card with data-testid', () => {
            const card = spectator.query('[data-testid="pageCard"]');
            expect(card).toBeTruthy();
        });

        it('should display image when imageUri is provided', () => {
            const img = spectator.query('img') as HTMLImageElement;
            const skeleton = spectator.query('dot-pages-favorite-page-empty-skeleton');

            expect(img).toBeTruthy();
            expect(skeleton).toBeNull();
            expect(img.alt).toBe('Test Page Title');
        });

        it('should render action button with ellipsis icon', () => {
            const button = spectator.query('p-button');
            expect(button).toBeTruthy();
            expect(button?.getAttribute('icon')).toBe('pi pi-ellipsis-v');
        });

        it('should render card with correct styling', () => {
            const card = spectator.query('p-card');
            expect(card).toBeTruthy();
            expect(card?.classList.contains('cursor-pointer')).toBe(true);
        });

        it('should have title with truncate class for overflow handling', () => {
            const titleElement = spectator.query('.truncate.w-full') as HTMLElement;
            expect(titleElement).toBeTruthy();
            expect(titleElement.classList.contains('truncate')).toBe(true);
        });

        it('should have url with truncate class for overflow handling', () => {
            const urlElement = spectator.query('.truncate.w-full.text-sm') as HTMLElement;
            expect(urlElement).toBeTruthy();
            expect(urlElement.classList.contains('truncate')).toBe(true);
        });
    });

    describe('Output Events', () => {
        it('should emit navigateToPage when card is clicked', () => {
            const card = spectator.query('[data-testid="pageCard"]') as HTMLElement;
            expect(card).toBeTruthy();

            spectator.click(card);
            spectator.detectChanges();

            expect(host().navigateToPageEmitted).toBe(true);
        });

        it('should emit openMenu when action button is clicked', () => {
            const button = spectator.query('p-button') as HTMLElement;
            expect(button).toBeTruthy();

            // Create a mock MouseEvent
            const mockEvent = new MouseEvent('click', {
                bubbles: true,
                cancelable: true
            });

            // Trigger the button's onClick event
            spectator.triggerEventHandler('p-button', 'onClick', mockEvent);
            spectator.detectChanges();

            expect(host().openMenuEmitted).toBeTruthy();
            expect(host().openMenuEmitted).toBeInstanceOf(MouseEvent);
        });

        it('should not emit navigateToPage when action button is clicked', () => {
            host().navigateToPageEmitted = null;
            host().openMenuEmitted = null;

            const mockEvent = new MouseEvent('click', {
                bubbles: true,
                cancelable: true
            });

            spectator.triggerEventHandler('p-button', 'onClick', mockEvent);
            spectator.detectChanges();
            // Should only emit openMenu, not navigateToPage
            expect(host().openMenuEmitted).toBeTruthy();
            expect(host().openMenuEmitted).toBeInstanceOf(MouseEvent);
            expect(host().navigateToPageEmitted).toBeNull();
        });
    });

    describe('Edge Cases - Default Values', () => {
        it('should maintain image alt text matching title', () => {
            const img = spectator.query('img') as HTMLImageElement;
            const titleElement = spectator.query('.truncate.w-full');

            expect(img).toBeTruthy();
            expect(titleElement).toBeTruthy();
            expect(img.alt).toBe(titleElement?.textContent?.trim());
        });
    });

    describe('Integration Workflows', () => {
        it('should handle complete card interaction workflow', () => {
            // Step 1: Verify card displays correctly
            const titleElement = spectator.query('.truncate.w-full');
            const urlElement = spectator.query('.truncate.w-full.text-sm');
            const imgElement = spectator.query('img') as HTMLImageElement;

            expect(titleElement?.textContent?.trim()).toBe('Test Page Title');
            expect(urlElement?.textContent?.trim()).toBe('/test-page-url');
            expect(imgElement).toBeTruthy();

            // Step 2: User clicks the card to navigate
            const card = spectator.query('[data-testid="pageCard"]') as HTMLElement;
            spectator.click(card);
            spectator.detectChanges();

            expect(host().navigateToPageEmitted).toBe(true);

            // Step 3: User clicks action menu
            const mockEvent = new MouseEvent('click');
            spectator.triggerEventHandler('p-button', 'onClick', mockEvent);
            spectator.detectChanges();

            expect(host().openMenuEmitted).toBeInstanceOf(MouseEvent);
        });
    });
});
