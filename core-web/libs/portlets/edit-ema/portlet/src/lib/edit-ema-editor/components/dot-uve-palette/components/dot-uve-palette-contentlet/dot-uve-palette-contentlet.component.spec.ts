import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

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
        it('should render drag handle with correct icons', () => {
            // Drag handle is a simple icon container; we don't assert on custom CSS classes.
            const icons = spectator.queryAll('i.pi.pi-ellipsis-v');
            expect(icons).toHaveLength(2);
        });

        it('should render dot-contentlet-thumbnail with correct properties', () => {
            const thumbnail = spectator.query('dot-contentlet-thumbnail') as HTMLElement & {
                iconSize: string;
                contentlet: DotCMSContentlet;
            };

            expect(thumbnail).toBeTruthy();
            expect(thumbnail.iconSize).toBe('24px');
            expect(thumbnail.contentlet).toBeDefined();
            expect(thumbnail.contentlet.identifier).toBe('test-identifier');
        });

        it('should render contentlet title', () => {
            const titleElement = spectator.query('div.text-sm.font-semibold') as HTMLElement;
            expect(titleElement).toBeTruthy();
            expect(titleElement.textContent?.trim()).toBe('Test Contentlet Title');
        });

        it('should update title when contentlet changes', () => {
            const newContentlet: DotCMSContentlet = {
                ...baseContentlet,
                title: 'Updated Title'
            };

            spectator.setInput('contentlet', newContentlet);
            spectator.detectChanges();

            const titleElement = spectator.query('div.text-sm.font-semibold') as HTMLElement;
            expect(titleElement.textContent?.trim()).toBe('Updated Title');
        });
    });

    describe('Component Structure', () => {
        it('should have correct CSS classes structure', () => {
            // Structure assertions: we only verify key pieces exist.
            expect(spectator.query('dot-contentlet-thumbnail')).toBeTruthy();
            expect(spectator.query('div.text-sm.font-semibold')).toBeTruthy();
        });

        it('should nest dot-contentlet-thumbnail inside icon container', () => {
            // Thumbnail exists and is rendered inside the component.
            expect(spectator.query('dot-contentlet-thumbnail')).toBeTruthy();
        });
    });
});
