import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import {
    DotMessageService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService
} from '@dotcms/data-access';
import { SeoMetaTagsResult, SeoMetaTags } from '@dotcms/dotcms-models';

import { DotUveIframeComponent } from './dot-uve-iframe.component';

import { InlineEditService } from '../../../services/inline-edit/inline-edit.service';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import { SDK_EDITOR_SCRIPT_SOURCE } from '../../../utils';

describe('DotUveIframeComponent', () => {
    let spectator: Spectator<DotUveIframeComponent>;
    let component: DotUveIframeComponent;
    let mockUVEStore: InstanceType<typeof UVEStore>;
    let mockDotSeoMetaTagsService: DotSeoMetaTagsService;
    let mockDotSeoMetaTagsUtilService: DotSeoMetaTagsUtilService;
    let mockInlineEditService: InlineEditService;

    const mockPageRender = '<html><head></head><body>Test Content</body></html>';
    const mockSeoResults: SeoMetaTagsResult[] = [
        {
            key: 'og:title',
            title: 'title',
            keyIcon: 'icon',
            keyColor: 'color',
            items: [],
            sort: 1
        }
    ];

    const mockOgTags: SeoMetaTags = {
        'og:title': 'Test OG Title',
        'og:description': 'Test OG Description',
        'og:image': 'https://example.com/image.jpg'
    };

    const createComponent = createComponentFactory({
        component: DotUveIframeComponent,
        providers: [
            MockProvider(DotMessageService, {
                get: (key: string) => {
                    const messages: Record<string, string> = {
                        'editpage.container.is.empty': 'Container is empty'
                    };
                    return messages[key] || key;
                }
            }),
            MockProvider(DotSeoMetaTagsService, {
                getMetaTagsResults: jest.fn().mockReturnValue(of(mockSeoResults))
            }),
            MockProvider(DotSeoMetaTagsUtilService, {
                getMetaTags: jest.fn().mockReturnValue(mockOgTags)
            }),
            MockProvider(InlineEditService, {
                injectInlineEdit: jest.fn(),
                removeInlineEdit: jest.fn()
            }),
            MockProvider(UVEStore, {
                $pageRender: signal(mockPageRender),
                $enableInlineEdit: signal(false),
                pageType: signal(PageType.HEADLESS),
                setOgTags: jest.fn(),
                setOGTagResults: jest.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                src: 'https://example.com/test',
                title: 'Test Iframe',
                pointerEvents: 'auto',
                opacity: 1,
                host: '*'
            }
        });
        component = spectator.component;

        mockUVEStore = spectator.inject(UVEStore, true);
        mockDotSeoMetaTagsService = spectator.inject(DotSeoMetaTagsService, true);
        mockDotSeoMetaTagsUtilService = spectator.inject(DotSeoMetaTagsUtilService, true);
        mockInlineEditService = spectator.inject(InlineEditService, true);
    });

    describe('Component Creation', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });

        it('should set inputs correctly', () => {
            expect(component.src).toBe('https://example.com/test');
            expect(component.title).toBe('Test Iframe');
            expect(component.pointerEvents).toBe('auto');
            expect(component.opacity).toBe(1);
            expect(component.host).toBe('*');
        });
    });

    describe('Iframe Element', () => {
        it('should render iframe with correct attributes', () => {
            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
            expect(iframe).toBeTruthy();
            expect(iframe.getAttribute('title')).toBe('Test Iframe');
            expect(iframe.getAttribute('sandbox')).toBe('allow-scripts allow-same-origin allow-forms');
        });

        it('should apply ngStyle correctly', () => {
            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
            expect(iframe.style.pointerEvents).toBe('auto');
            expect(iframe.style.opacity).toBe('1');
        });

        it('should update styles when inputs change', () => {
            spectator.setInput('pointerEvents', 'none');
            spectator.setInput('opacity', 0.5);
            spectator.detectChanges();

            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
            expect(iframe.style.pointerEvents).toBe('none');
            expect(iframe.style.opacity).toBe('0.5');
        });

        it('should have contentWindow getter', () => {
            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
            // Mock contentWindow
            Object.defineProperty(iframe, 'contentWindow', {
                value: window,
                writable: true
            });

            expect(component.contentWindow).toBeTruthy();
        });

        it('should return null if iframe is not available', () => {
            component.iframe = undefined as any;
            expect(component.contentWindow).toBeNull();
            expect(component.iframeElement).toBeNull();
        });

        it('should have iframeElement getter', () => {
            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
            expect(component.iframeElement).toBe(iframe);
        });
    });

    describe('onIframeLoad - HEADLESS page type', () => {
        beforeEach(() => {
            mockUVEStore.pageType = signal(PageType.HEADLESS);
        });

        it('should emit load event for HEADLESS page type', () => {
            const loadSpy = jest.spyOn(component.load, 'emit');
            component.onIframeLoad();
            expect(loadSpy).toHaveBeenCalledTimes(1);
        });

        it('should not insert page content for HEADLESS page type', () => {
            const insertSpy = jest.spyOn(component as any, 'insertPageContent');
            component.onIframeLoad();
            expect(insertSpy).not.toHaveBeenCalled();
        });
    });

    describe('onIframeLoad - TRADITIONAL page type', () => {
        let mockIframe: HTMLIFrameElement;
        let mockDoc: Document;
        let mockWindow: Window;

        beforeEach(() => {
            mockUVEStore.pageType = signal(PageType.TRADITIONAL);
            mockUVEStore.$pageRender = signal(mockPageRender);
            mockUVEStore.$enableInlineEdit = signal(false);

            // Create mock iframe with contentDocument and contentWindow
            mockIframe = document.createElement('iframe');
            mockDoc = document.implementation.createHTMLDocument();
            mockWindow = {
                addEventListener: jest.fn(),
                removeEventListener: jest.fn()
            } as unknown as Window;

            Object.defineProperty(mockIframe, 'contentDocument', {
                value: mockDoc,
                writable: true
            });
            Object.defineProperty(mockIframe, 'contentWindow', {
                value: mockWindow,
                writable: true
            });

            component.iframe = { nativeElement: mockIframe } as any;
        });

        it('should emit load event for TRADITIONAL page type', () => {
            const loadSpy = jest.spyOn(component.load, 'emit');
            component.onIframeLoad();
            expect(loadSpy).toHaveBeenCalledTimes(1);
        });

        it('should insert page content for TRADITIONAL page type', () => {
            const insertSpy = jest.spyOn(component as any, 'insertPageContent');
            component.onIframeLoad();
            expect(insertSpy).toHaveBeenCalledWith(mockPageRender, false);
        });

        it('should set SEO data for TRADITIONAL page type', () => {
            const setSeoSpy = jest.spyOn(component as any, 'setSeoData');
            component.onIframeLoad();
            expect(setSeoSpy).toHaveBeenCalledTimes(1);
        });

        it('should write content to iframe document', () => {
            const openSpy = jest.spyOn(mockDoc, 'open');
            const writeSpy = jest.spyOn(mockDoc, 'write');
            const closeSpy = jest.spyOn(mockDoc, 'close');

            component.onIframeLoad();

            expect(openSpy).toHaveBeenCalledTimes(1);
            expect(writeSpy).toHaveBeenCalledTimes(1);
            expect(closeSpy).toHaveBeenCalledTimes(1);
        });

        it('should not insert content if iframe element is not available', () => {
            component.iframe = undefined as any;
            const openSpy = jest.spyOn(mockDoc, 'open');
            component.onIframeLoad();
            expect(openSpy).not.toHaveBeenCalled();
        });

        it('should not insert content if contentDocument is not available', () => {
            Object.defineProperty(mockIframe, 'contentDocument', {
                value: null,
                writable: true
            });
            const openSpy = jest.spyOn(mockDoc, 'open');
            component.onIframeLoad();
            expect(openSpy).not.toHaveBeenCalled();
        });
    });

    describe('insertPageContent - Code Injection', () => {
        let mockIframe: HTMLIFrameElement;
        let mockDoc: Document;
        let mockWindow: Window;
        let writeSpy: jest.SpyInstance;

        beforeEach(() => {
            mockIframe = document.createElement('iframe');
            mockDoc = document.implementation.createHTMLDocument();
            mockWindow = {
                addEventListener: jest.fn(),
                removeEventListener: jest.fn()
            } as unknown as Window;

            // Spy on document write method
            writeSpy = jest.spyOn(mockDoc, 'write').mockImplementation(() => {});

            Object.defineProperty(mockIframe, 'contentDocument', {
                value: mockDoc,
                writable: true
            });
            Object.defineProperty(mockIframe, 'contentWindow', {
                value: mockWindow,
                writable: true
            });

            component.iframe = { nativeElement: mockIframe } as any;
            mockUVEStore.$enableInlineEdit = signal(false);
        });

        afterEach(() => {
            writeSpy.mockRestore();
        });

        it('should inject editor script before closing body tag', () => {
            const htmlWithBody = '<html><head></head><body>Content</body></html>';
            (component as any).insertPageContent(htmlWithBody, false);

            const writtenContent = writeSpy.mock.calls[0][0];
            expect(writtenContent).toContain(SDK_EDITOR_SCRIPT_SOURCE);
            expect(writtenContent).toContain('</body>');
            expect(writtenContent.indexOf(SDK_EDITOR_SCRIPT_SOURCE)).toBeLessThan(
                writtenContent.indexOf('</body>')
            );
        });

        it('should inject editor script at end if no body tag exists', () => {
            const htmlWithoutBody = '<html><head></head></html>';
            writeSpy.mockClear();
            (component as any).insertPageContent(htmlWithoutBody, false);

            const writtenContent = writeSpy.mock.calls[0][0];
            expect(writtenContent).toContain(SDK_EDITOR_SCRIPT_SOURCE);
            // Script is added at the end, then styles are added before </head>
            // So script should appear after </head>
            expect(writtenContent.indexOf(SDK_EDITOR_SCRIPT_SOURCE)).toBeGreaterThan(
                writtenContent.indexOf('</head>')
            );
        });

        it('should inject custom styles before closing head tag', () => {
            const htmlWithHead = '<html><head></head><body>Content</body></html>';
            (component as any).insertPageContent(htmlWithHead, false);

            const writtenContent = writeSpy.mock.calls[0][0];
            expect(writtenContent).toContain('<style>');
            expect(writtenContent).toContain('Container is empty');
            expect(writtenContent).toContain('</head>');
        });

        it('should inject custom styles at end if no head tag exists', () => {
            const htmlWithoutHead = '<html><body>Content</body></html>';
            writeSpy.mockClear();
            (component as any).insertPageContent(htmlWithoutHead, false);

            const writtenContent = writeSpy.mock.calls[0][0];
            expect(writtenContent).toContain('<style>');
            // Script is added before </body>, styles are added at the end after </html>
            // So styles should appear after </html>
            expect(writtenContent.indexOf('<style>')).toBeGreaterThan(writtenContent.indexOf('</html>'));
        });
    });

    describe('handleInlineScripts', () => {
        let mockIframe: HTMLIFrameElement;
        let mockWindow: Window;
        let mockClickEvent: MouseEvent;

        beforeEach(() => {
            mockIframe = document.createElement('iframe');
            mockWindow = {
                addEventListener: jest.fn(),
                removeEventListener: jest.fn()
            } as unknown as Window;
            mockClickEvent = new MouseEvent('click', { bubbles: true });

            Object.defineProperty(mockIframe, 'contentWindow', {
                value: mockWindow,
                writable: true
            });

            component.iframe = { nativeElement: mockIframe } as any;
        });

        it('should subscribe to click events on iframe window', () => {
            (mockWindow.addEventListener as jest.Mock).mockClear();
            (component as any).handleInlineScripts(false);
            expect(mockWindow.addEventListener).toHaveBeenCalled();
            expect((mockWindow.addEventListener as jest.Mock).mock.calls[0][0]).toBe('click');
            expect(typeof (mockWindow.addEventListener as jest.Mock).mock.calls[0][1]).toBe('function');
        });

        it('should emit internalNav on click', () => {
            const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
            let clickHandler: ((e: MouseEvent) => void) | undefined;

            (mockWindow.addEventListener as jest.Mock).mockImplementation((event: string, handler: (e: MouseEvent) => void) => {
                if (event === 'click') {
                    clickHandler = handler;
                }
            });

            (component as any).handleInlineScripts(false);

            if (clickHandler) {
                clickHandler(mockClickEvent);
                expect(internalNavSpy).toHaveBeenCalledWith(mockClickEvent);
            }
        });

        it('should emit inlineEditing on click', () => {
            const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');
            let clickHandler: ((e: MouseEvent) => void) | undefined;

            (mockWindow.addEventListener as jest.Mock).mockImplementation((event: string, handler: (e: MouseEvent) => void) => {
                if (event === 'click') {
                    clickHandler = handler;
                }
            });

            (component as any).handleInlineScripts(false);

            if (clickHandler) {
                clickHandler(mockClickEvent);
                expect(inlineEditingSpy).toHaveBeenCalledWith(mockClickEvent);
            }
        });

        it('should inject inline edit when enabled', () => {
            (component as any).handleInlineScripts(true);
            expect(mockInlineEditService.injectInlineEdit).toHaveBeenCalledWith(component.iframe);
        });

        it('should remove inline edit when disabled', () => {
            (component as any).handleInlineScripts(false);
            expect(mockInlineEditService.removeInlineEdit).toHaveBeenCalledWith(component.iframe);
        });

        it('should not handle scripts if contentWindow is not available', () => {
            (mockInlineEditService.injectInlineEdit as jest.Mock).mockClear();
            component.iframe = undefined as any;
            (component as any).handleInlineScripts(true);
            expect(mockInlineEditService.injectInlineEdit).not.toHaveBeenCalled();
        });
    });

    describe('setSeoData', () => {
        let mockIframe: HTMLIFrameElement;
        let mockDoc: Document;

        beforeEach(() => {
            mockIframe = document.createElement('iframe');
            mockDoc = document.implementation.createHTMLDocument();

            Object.defineProperty(mockIframe, 'contentDocument', {
                value: mockDoc,
                writable: true
            });

            component.iframe = { nativeElement: mockIframe } as any;
        });

        it('should get meta tags results', () => {
            (component as any).setSeoData();
            expect(mockDotSeoMetaTagsService.getMetaTagsResults).toHaveBeenCalledWith(mockDoc);
        });

        it('should get OG tags', () => {
            (component as any).setSeoData();
            expect(mockDotSeoMetaTagsUtilService.getMetaTags).toHaveBeenCalledWith(mockDoc);
        });

        it('should set OG tags in store', () => {
            (component as any).setSeoData();
            // Wait for observable to complete
            setTimeout(() => {
                expect(mockUVEStore.setOgTags).toHaveBeenCalledWith(mockOgTags);
            }, 0);
        });

        it('should set OG tag results in store', () => {
            (component as any).setSeoData();
            // Wait for observable to complete
            setTimeout(() => {
                expect(mockUVEStore.setOGTagResults).toHaveBeenCalledWith(mockSeoResults);
            }, 0);
        });

        it('should not set SEO data if iframe element is not available', () => {
            (mockDotSeoMetaTagsService.getMetaTagsResults as jest.Mock).mockClear();
            component.iframe = undefined as any;
            (component as any).setSeoData();
            expect(mockDotSeoMetaTagsService.getMetaTagsResults).not.toHaveBeenCalled();
        });

        it('should not set SEO data if contentDocument is not available', () => {
            (mockDotSeoMetaTagsService.getMetaTagsResults as jest.Mock).mockClear();
            Object.defineProperty(mockIframe, 'contentDocument', {
                value: null,
                writable: true
            });
            (component as any).setSeoData();
            expect(mockDotSeoMetaTagsService.getMetaTagsResults).not.toHaveBeenCalled();
        });
    });

    describe('Effect - $isTraditionalPageEffect', () => {
        let mockIframe: HTMLIFrameElement;
        let mockDoc: Document;

        beforeEach(() => {
            mockIframe = document.createElement('iframe');
            mockDoc = document.implementation.createHTMLDocument();

            Object.defineProperty(mockIframe, 'contentDocument', {
                value: mockDoc,
                writable: true
            });

            component.iframe = { nativeElement: mockIframe } as any;
            mockUVEStore.$pageRender = signal(mockPageRender);
            mockUVEStore.$enableInlineEdit = signal(false);
        });

        it('should insert page content when pageType changes to TRADITIONAL', () => {
            mockUVEStore.pageType = signal(PageType.TRADITIONAL);
            const insertSpy = jest.spyOn(component as any, 'insertPageContent');

            // Trigger effect by accessing the signal
            mockUVEStore.pageType();
            spectator.detectChanges();

            // Effect should run on next tick
            setTimeout(() => {
                expect(insertSpy).toHaveBeenCalled();
            }, 0);
        });

        it('should not insert page content when pageType is HEADLESS', () => {
            mockUVEStore.pageType = signal(PageType.HEADLESS);
            const insertSpy = jest.spyOn(component as any, 'insertPageContent');

            mockUVEStore.pageType();
            spectator.detectChanges();

            setTimeout(() => {
                expect(insertSpy).not.toHaveBeenCalled();
            }, 0);
        });
    });

    describe('Output Events', () => {
        it('should emit load event', () => {
            const loadSpy = jest.spyOn(component.load, 'emit');
            component.onIframeLoad();
            expect(loadSpy).toHaveBeenCalledTimes(1);
        });

        it('should emit internalNav event', () => {
            const mockEvent = new MouseEvent('click');
            const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
            component.internalNav.emit(mockEvent);
            expect(internalNavSpy).toHaveBeenCalledWith(mockEvent);
        });

        it('should emit inlineEditing event', () => {
            const mockEvent = new MouseEvent('click');
            const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');
            component.inlineEditing.emit(mockEvent);
            expect(inlineEditingSpy).toHaveBeenCalledWith(mockEvent);
        });
    });
});
