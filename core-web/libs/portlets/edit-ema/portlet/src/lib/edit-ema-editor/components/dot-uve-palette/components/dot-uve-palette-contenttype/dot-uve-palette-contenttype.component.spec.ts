import {
    createComponentFactory,
    createHostFactory,
    Spectator,
    SpectatorHost
} from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { By } from '@angular/platform-browser';

import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';

import { DotUVEPaletteContenttypeComponent } from './dot-uve-palette-contenttype.component';

import { DotCMSPaletteContentType } from '../../models';

@Component({
    selector: 'dot-test-host',
    standalone: false,
    template: `
        <dot-uve-palette-contenttype [contentType]="contentType" [view]="view" />
    `
})
class TestHostComponent {
    view: 'grid' | 'list' = 'grid';
    contentType: DotCMSPaletteContentType = {
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
        workflows: [],
        disabled: false
    };
}

describe('DotUVEPaletteContenttypeComponent', () => {
    let spectator: SpectatorHost<DotUVEPaletteContenttypeComponent, TestHostComponent>;
    let componentSpectator: Spectator<DotUVEPaletteContenttypeComponent>;

    const createHost = createHostFactory({
        component: DotUVEPaletteContenttypeComponent,
        host: TestHostComponent,
        imports: [DotUVEPaletteContenttypeComponent],
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    // Keep it deterministic for tests: return the key as-is
                    get: jest.fn((key: string) => key)
                }
            }
        ]
    });

    const createComponent = createComponentFactory({
        component: DotUVEPaletteContenttypeComponent,
        imports: [DotUVEPaletteContenttypeComponent],
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    // Keep it deterministic for tests: return the key as-is
                    get: jest.fn((key: string) => key)
                }
            }
        ],
        detectChanges: false
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
            const newContentType: DotCMSPaletteContentType = {
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
        // NOTE: host styling is handled by $hostClass(); we don't assert on CSS classes.
    });

    describe('Template Rendering', () => {
        it('should render drag handle with correct icons', () => {
            const dragHandle = spectator.query('.drag-handle');
            const icons = spectator.queryAll('.drag-handle i');

            expect(dragHandle).toBeTruthy();
            expect(icons).toHaveLength(2);
        });

        it('should render content type icon when icon is provided', () => {
            const iconElement = spectator.query('i.material-icons');

            expect(iconElement).toBeTruthy();
            expect(iconElement?.textContent?.trim()).toBe('article');
        });

        it('should render default palette icon when no icon is provided', () => {
            const newContentType: DotCMSPaletteContentType = {
                ...spectator.hostComponent.contentType,
                icon: undefined
            };

            spectator.hostComponent.contentType = newContentType;
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();

            const iconElement = spectator.query('i.material-icons');

            expect(iconElement).toBeTruthy();
            expect(iconElement?.textContent?.trim()).toBe('palette');
        });

        it('should render content type name', () => {
            // The name is rendered as a plain <div> (no dedicated ".name" class anymore)
            const nameElement = spectator.query('div.text-sm.font-semibold') as HTMLElement;

            expect(nameElement).toBeTruthy();
            expect(nameElement?.textContent?.trim()).toBe('Test Content Type');
        });

        it('should update name when contentType changes', () => {
            const newContentType: DotCMSPaletteContentType = {
                ...spectator.hostComponent.contentType,
                name: 'Updated Content Type'
            };

            spectator.hostComponent.contentType = newContentType;
            spectator.hostFixture.changeDetectorRef.markForCheck();
            spectator.hostFixture.detectChanges();

            const nameElement = spectator.query('div.text-sm.font-semibold') as HTMLElement;
            expect(nameElement?.textContent?.trim()).toBe('Updated Content Type');
        });

        it('should render chevron icon', () => {
            const chevron = spectator.query('.chevron');
            const chevronIcon = spectator.query('.chevron i');

            expect(chevron).toBeTruthy();
            expect(chevronIcon).toBeTruthy();
        });
    });

    describe('Tooltip behavior', () => {
        it('should enable tooltip when contentType is disabled', () => {
            const disabledContentType: DotCMSPaletteContentType = {
                ...spectator.hostComponent.contentType,
                disabled: true
            };

            componentSpectator = createComponent({
                props: { contentType: disabledContentType, view: 'grid' }
            });
            componentSpectator.detectChanges();

            const tooltipDebugEl = componentSpectator.fixture.debugElement.query(
                By.directive(Tooltip)
            );
            expect(tooltipDebugEl).toBeTruthy();
            const tooltip = tooltipDebugEl.injector.get(Tooltip);

            expect(tooltip).toBeTruthy();
            expect(tooltip.disabled).toBe(false);
            expect(tooltip.content).toBe('uve.palette.item.disabled.tooltip');
        });

        it('should disable tooltip when contentType is not disabled', () => {
            const enabledContentType: DotCMSPaletteContentType = {
                ...spectator.hostComponent.contentType,
                disabled: false
            };

            componentSpectator = createComponent({
                props: { contentType: enabledContentType, view: 'grid' }
            });
            componentSpectator.detectChanges();

            const tooltipDebugEl = componentSpectator.fixture.debugElement.query(
                By.directive(Tooltip)
            );
            expect(tooltipDebugEl).toBeTruthy();
            const tooltip = tooltipDebugEl.injector.get(Tooltip);

            expect(tooltip).toBeTruthy();
            expect(tooltip.disabled).toBe(true);
            expect(tooltip.content).toBe('uve.palette.item.disabled.tooltip');
        });
    });

    describe('Component Structure', () => {
        it('should have correct CSS classes structure', () => {
            expect(spectator.query('.drag-handle')).toBeTruthy();
            expect(spectator.query('i.material-icons')).toBeTruthy();
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
            const contextMenuSpy = jest.fn();
            spectator.output('contextMenu').subscribe(contextMenuSpy);

            const element = spectator.element as HTMLElement;
            element.dispatchEvent(mockEvent);

            expect(preventDefaultSpy).toHaveBeenCalled();
            expect(contextMenuSpy).toHaveBeenCalled();
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
