import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotUvePaletteContentletComponent } from './dot-uve-palette-contentlet.component';

@Component({
    selector: 'dot-test-host',
    standalone: false,
    template: `
        <dot-uve-palette-contentlet [contentlet]="contentlet" />
    `
})
class TestHostComponent {
    contentlet: DotCMSContentlet = {
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
}

describe('DotUvePaletteContentletComponent', () => {
    let spectator: SpectatorHost<DotUvePaletteContentletComponent, TestHostComponent>;

    const createHost = createHostFactory({
        component: DotUvePaletteContentletComponent,
        host: TestHostComponent,
        imports: [DotUvePaletteContentletComponent],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    beforeEach(() => {
        spectator = createHost(`<dot-uve-palette-contentlet [contentlet]="contentlet" />`);
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
                ...spectator.hostComponent.contentlet,
                identifier: 'new-identifier',
                title: 'New Title'
            };

            spectator.setHostInput({ contentlet: newContentlet });
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
            const dragHandle = spectator.query('.drag-handle');
            const icons = spectator.queryAll('.drag-handle i');

            expect(dragHandle).toBeTruthy();
            expect(icons).toHaveLength(2);
            expect(icons[0]).toHaveClass('pi');
            expect(icons[0]).toHaveClass('pi-ellipsis-v');
            expect(icons[1]).toHaveClass('pi');
            expect(icons[1]).toHaveClass('pi-ellipsis-v');
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
            const nameElement = spectator.query('.content .name');

            expect(nameElement).toBeTruthy();
            expect(nameElement?.textContent?.trim()).toBe('Test Contentlet Title');
        });

        it('should update title when contentlet changes', () => {
            const newContentlet: DotCMSContentlet = {
                ...spectator.hostComponent.contentlet,
                title: 'Updated Title'
            };

            spectator.setHostInput({ contentlet: newContentlet });
            spectator.detectChanges();

            const nameElement = spectator.query('.content .name');
            expect(nameElement?.textContent?.trim()).toBe('Updated Title');
        });
    });

    describe('Component Structure', () => {
        it('should have correct CSS classes structure', () => {
            expect(spectator.query('.drag-handle')).toBeTruthy();
            expect(spectator.query('.content')).toBeTruthy();
            expect(spectator.query('.content .icon')).toBeTruthy();
            expect(spectator.query('.content .name')).toBeTruthy();
        });

        it('should nest dot-contentlet-thumbnail inside icon container', () => {
            const iconContainer = spectator.query('.content .icon');
            const thumbnail = iconContainer?.querySelector('dot-contentlet-thumbnail');

            expect(thumbnail).toBeTruthy();
        });
    });
});
