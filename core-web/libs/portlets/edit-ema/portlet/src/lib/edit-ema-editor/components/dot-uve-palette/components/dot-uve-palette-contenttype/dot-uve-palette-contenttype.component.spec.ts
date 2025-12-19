import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotUVEPaletteContenttypeComponent } from './dot-uve-palette-contenttype.component';

@Component({
    selector: 'dot-test-host',
    standalone: false,
    template: `
        <dot-uve-palette-contenttype [contentType]="contentType" [view]="view" />
    `
})
class TestHostComponent {
    view: 'grid grid-cols-12 gap-4' | 'list' = 'grid grid-cols-12 gap-4';
    contentType: DotCMSContentType = {
        baseType: 'CONTENT',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        detailPage: '',
        expireDateVar: null,
        fields: [],
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: 'test-host-id',
        icon: 'article',
        id: 'test-content-type-id',
        iDate: 1234567890,
        layout: [],
        modDate: 1234567891,
        multilingualable: false,
        name: 'Test Content Type',
        nEntries: 5,
        owner: 'test-owner',
        publishDateVar: null,
        system: false,
        systemActionMappings: {},
        variable: 'TestContentType',
        versionable: true,
        workflows: []
    };
}

describe('DotUVEPaletteContenttypeComponent', () => {
    let spectator: SpectatorHost<DotUVEPaletteContenttypeComponent, TestHostComponent>;

    const createHost = createHostFactory({
        component: DotUVEPaletteContenttypeComponent,
        host: TestHostComponent,
        imports: [DotUVEPaletteContenttypeComponent]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dot-uve-palette-contenttype [contentType]="contentType" [view]="view" />`
        );
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('ContentType Host Attributes', () => {
        it('should have data-type attribute set to "content-type"', () => {
            const element = spectator.element as HTMLElement;
            expect(element.getAttribute('data-type')).toBe('content-type');
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
                contentType: {
                    variable: 'TestContentType',
                    name: 'Test Content Type',
                    baseType: 'CONTENT'
                },
                move: false
            });
        });

        it('should update data-item attribute when contentType changes', () => {
            const newContentType: DotCMSContentType = {
                ...spectator.hostComponent.contentType,
                variable: 'NewVariable',
                name: 'New Content Type Name'
            };

            spectator.hostComponent.contentType = newContentType;
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();

            const element = spectator.element as HTMLElement;
            const dataItem = element.getAttribute('data-item') as string;

            expect(dataItem).toBeTruthy();
            const parsedData = JSON.parse(dataItem);

            expect(parsedData.contentType.variable).toBe('NewVariable');
            expect(parsedData.contentType.name).toBe('New Content Type Name');
        });
    });

    describe('View Input and CSS Classes', () => {
        it('should not have list-view class when view is "grid"', () => {
            // Default view is 'grid', so no need to change input
            const element = spectator.element as HTMLElement;
            expect(element.classList.contains('list-view')).toBe(false);
        });

        it('should have list-view class when view is "list"', () => {
            spectator.hostComponent.view = 'list';
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();

            const element = spectator.element as HTMLElement;
            expect(element.classList.contains('list-view')).toBe(true);
        });

        it('should toggle list-view class when view changes', () => {
            const element = spectator.element as HTMLElement;

            spectator.hostComponent.view = 'grid grid-cols-12 gap-4';
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();
            expect(element.classList.contains('list-view')).toBe(false);

            spectator.hostComponent.view = 'list';
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();
            expect(element.classList.contains('list-view')).toBe(true);

            spectator.hostComponent.view = 'grid grid-cols-12 gap-4';
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();
            expect(element.classList.contains('list-view')).toBe(false);
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

        it('should render content type icon when icon is provided', () => {
            const iconElement = spectator.query('.content .icon i');

            expect(iconElement).toBeTruthy();
            expect(iconElement).toHaveClass('material-icons');
            expect(iconElement).toHaveClass('material-icons-outlined');
            expect(iconElement?.textContent?.trim()).toBe('article');
        });

        it('should render default palette icon when no icon is provided', () => {
            const newContentType: DotCMSContentType = {
                ...spectator.hostComponent.contentType,
                icon: undefined
            };

            spectator.hostComponent.contentType = newContentType;
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();

            const iconElement = spectator.query('.content .icon i');

            expect(iconElement).toBeTruthy();
            expect(iconElement).toHaveClass('material-icons');
            expect(iconElement).toHaveClass('material-icons-outlined');
            expect(iconElement?.textContent?.trim()).toBe('palette');
        });

        it('should render content type name', () => {
            const nameElement = spectator.query('.content .name');

            expect(nameElement).toBeTruthy();
            expect(nameElement?.textContent?.trim()).toBe('Test Content Type');
        });

        it('should update name when contentType changes', () => {
            const newContentType: DotCMSContentType = {
                ...spectator.hostComponent.contentType,
                name: 'Updated Content Type'
            };

            spectator.hostComponent.contentType = newContentType;
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();

            const nameElement = spectator.query('.content .name');
            expect(nameElement?.textContent?.trim()).toBe('Updated Content Type');
        });

        it('should render chevron icon', () => {
            const chevron = spectator.query('.chevron');
            const chevronIcon = spectator.query('.chevron i');

            expect(chevron).toBeTruthy();
            expect(chevronIcon).toBeTruthy();
            expect(chevronIcon).toHaveClass('pi');
            expect(chevronIcon).toHaveClass('pi-chevron-right');
        });
    });

    describe('Component Structure', () => {
        it('should have correct CSS classes structure', () => {
            expect(spectator.query('.drag-handle')).toBeTruthy();
            expect(spectator.query('.content')).toBeTruthy();
            expect(spectator.query('.content .icon')).toBeTruthy();
            expect(spectator.query('.content .name')).toBeTruthy();
            expect(spectator.query('.chevron')).toBeTruthy();
        });
    });

    describe('Output Events', () => {
        it('should emit selectContentType when chevron is clicked', (done) => {
            spectator.output('onSelectContentType').subscribe((value: string) => {
                expect(value).toBe('TestContentType');
                done();
            });

            const chevron = spectator.query('.chevron');
            spectator.click(chevron as Element);
        });

        it('should emit correct variable when contentType changes and chevron is clicked', (done) => {
            const newContentType: DotCMSContentType = {
                ...spectator.hostComponent.contentType,
                variable: 'NewVariableName'
            };

            spectator.hostComponent.contentType = newContentType;
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();

            spectator.output('onSelectContentType').subscribe((value: string) => {
                expect(value).toBe('NewVariableName');
                done();
            });

            const chevron = spectator.query('.chevron');
            spectator.click(chevron as Element);
        });
    });

    describe('Right Click Event', () => {
        it('should emit rightClick event when component is right-clicked', (done) => {
            spectator.output('contextMenu').subscribe((event: MouseEvent) => {
                expect(event).toBeInstanceOf(MouseEvent);
                done();
            });

            const element = spectator.element as HTMLElement;
            const event = new MouseEvent('contextmenu', {
                bubbles: true,
                cancelable: true
            });

            element.dispatchEvent(event);
        });

        it('should prevent default behavior on right-click', () => {
            const mockEvent = new MouseEvent('contextmenu', {
                bubbles: true,
                cancelable: true
            });

            const preventDefaultSpy = jest.spyOn(mockEvent, 'preventDefault');

            spectator.output('contextMenu').subscribe(() => {
                fail('rightClick should not be emitted');
            });

            const element = spectator.element as HTMLElement;
            element.dispatchEvent(mockEvent);

            expect(preventDefaultSpy).toHaveBeenCalled();
        });

        it('should emit rightClick with correct event type', (done) => {
            spectator.output('contextMenu').subscribe((event: MouseEvent) => {
                expect(event).toBeInstanceOf(MouseEvent);
                expect(event.type).toBe('contextmenu');
                done();
            });

            const mockEvent = new MouseEvent('contextmenu', {
                bubbles: true,
                cancelable: true,
                clientX: 100,
                clientY: 200
            });

            const element = spectator.element as HTMLElement;
            element.dispatchEvent(mockEvent);
        });
    });
});
