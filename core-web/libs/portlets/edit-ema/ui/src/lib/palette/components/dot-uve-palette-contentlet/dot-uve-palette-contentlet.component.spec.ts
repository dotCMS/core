import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';

import { Tooltip } from 'primeng/tooltip';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotUvePaletteContentletComponent } from './dot-uve-palette-contentlet.component';

describe('DotUvePaletteContentletComponent', () => {
    let spectator: Spectator<DotUvePaletteContentletComponent>;

    const createComponent = createComponentFactory({
        component: DotUvePaletteContentletComponent,
        imports: [DotUvePaletteContentletComponent],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false
    });

    const baseContentlet: DotCMSContentlet = {
        identifier: 'test-identifier',
        contentType: 'TestContentType',
        baseType: 'CONTENT',
        inode: 'test-inode',
        title: 'Test Contentlet Title',
        url: '/test/url',
        archived: false,
        folder: 'folder-id',
        hasTitleImage: false,
        host: 'host-id',
        hostName: 'test-host',
        languageId: 1,
        live: true,
        locked: false,
        modDate: '2024-01-01',
        modUser: 'user-id',
        modUserName: 'Test User',
        owner: 'owner-id',
        sortOrder: 0,
        stInode: 'st-inode',
        titleImage: 'test-image.jpg',
        working: true
    };

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: baseContentlet
                // Quick way to avoid type errors when passing the baseContentlet to the component since we are not prefixing with $
                // The ideal would be using a host component and pass the baseContentlet as a property to the host component
            } as unknown
        });
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Contentlet Host Attributes', () => {
        it('should have data-type attribute set to "contentlet"', () => {
            const element = spectator.element as HTMLElement;
            expect(element.getAttribute('data-type')).toBe('contentlet');
        });

        it('should have draggable attribute set to true', () => {
            const element = spectator.element as HTMLElement;
            expect(element.getAttribute('draggable')).toBe('true');
        });

        it('should not expose the browser default title tooltip on the contentlet block', () => {
            const element = spectator.element as HTMLElement;
            expect(element.hasAttribute('title')).toBe(false);
        });

        it('should have data-item attribute with correct JSON structure', () => {
            const element = spectator.element as HTMLElement;
            const dataItem = element.getAttribute('data-item') as string;

            expect(dataItem).toBeTruthy();
            const parsedData = JSON.parse(dataItem);
            expect(parsedData).toEqual({
                contentlet: {
                    identifier: 'test-identifier',
                    contentType: 'TestContentType',
                    baseType: 'CONTENT',
                    inode: 'test-inode',
                    title: 'Test Contentlet Title'
                },
                move: false
            });
        });

        it('should update data-item attribute when contentlet changes', () => {
            const newContentlet: DotCMSContentlet = {
                ...baseContentlet,
                identifier: 'new-identifier',
                title: 'New Title'
            };

            spectator.setInput('contentlet', newContentlet);
            spectator.detectChanges();

            const element = spectator.element as HTMLElement;
            const dataItem = element.getAttribute('data-item') as string;

            expect(dataItem).toBeTruthy();
            const parsedData = JSON.parse(dataItem);

            expect(parsedData.contentlet.identifier).toBe('new-identifier');
            expect(parsedData.contentlet.title).toBe('New Title');
        });
    });

    describe('Template Rendering', () => {
        it('should render dot-contentlet-thumbnail with correct properties', () => {
            const thumbnail = spectator.query(
                '[data-testid="contentlet-thumbnail"]'
            ) as HTMLElement & {
                iconSize: string;
                contentlet: DotCMSContentlet;
            };

            expect(thumbnail).toBeTruthy();
            expect(thumbnail.contentlet).toBeDefined();
            expect(thumbnail.contentlet.identifier).toBe('test-identifier');
        });

        it('should render contentlet title', () => {
            const titleElement = spectator.query('[data-testid="contentlet-title"]') as HTMLElement;
            expect(titleElement).toBeTruthy();
            expect(titleElement.textContent?.trim()).toBe('Test Contentlet Title');
            expect(titleElement).toHaveClass('line-clamp-2');
            expect(titleElement).toHaveClass('wrap-break-word');
        });

        it('should expose the full title through a PrimeNG tooltip on the title element', () => {
            const titleElement = spectator.query('[data-testid="contentlet-title"]') as HTMLElement;
            const tooltipDebugEl = spectator.fixture.debugElement.query(By.directive(Tooltip));
            const tooltip = tooltipDebugEl.injector.get(Tooltip);

            expect(tooltipDebugEl).toBeTruthy();
            expect(tooltipDebugEl.nativeElement).toBe(titleElement);
            expect(tooltip.content).toBe('Test Contentlet Title');
        });

        it('should update title when contentlet changes', () => {
            const newContentlet: DotCMSContentlet = {
                ...baseContentlet,
                title: 'Updated Title'
            };

            spectator.setInput('contentlet', newContentlet);
            spectator.detectChanges();

            const titleElement = spectator.query('[data-testid="contentlet-title"]') as HTMLElement;
            expect(titleElement.textContent?.trim()).toBe('Updated Title');
        });
    });

    describe('Component Structure', () => {
        it('should have correct CSS classes structure', () => {
            expect(spectator.query('[data-testid="contentlet-thumbnail"]')).toBeTruthy();
            expect(spectator.query('[data-testid="contentlet-title"]')).toBeTruthy();
        });
    });
});
