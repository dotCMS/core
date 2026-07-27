/* eslint-disable @typescript-eslint/no-explicit-any */
import { createComponentFactory, mockProvider, Spectator, byTestId } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal, WritableSignal } from '@angular/core';

import { DotSeoMetaTagsService, DotSeoMetaTagsUtilService } from '@dotcms/data-access';
import { SeoMetaTagsResult, SeoMetaTags } from '@dotcms/dotcms-models';

import { DotUveIframeComponent } from './dot-uve-iframe.component';

import { InlineEditService } from '../../../services/inline-edit/inline-edit.service';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import { SDK_EDITOR_SCRIPT_SOURCE } from '../../../utils/ema-legacy-script-injection';

describe('DotUveIframeComponent', () => {
    let spectator: Spectator<DotUveIframeComponent>;
    let component: DotUveIframeComponent;
    let mockUVEStore: InstanceType<typeof UVEStore>;
    let mockDotSeoMetaTagsService: DotSeoMetaTagsService;
    let mockDotSeoMetaTagsUtilService: DotSeoMetaTagsUtilService;
    let mockInlineEditService: InlineEditService;

    let pageTypeSignal: WritableSignal<PageType>;
    let pageRenderSignal: WritableSignal<string>;
    let editorEnableInlineEditSignal: WritableSignal<boolean>;
    let legacyScriptInjectionEnabledSignal: WritableSignal<boolean>;

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
            mockProvider(DotSeoMetaTagsService, {
                getMetaTagsResults: jest.fn().mockReturnValue(of(mockSeoResults))
            }),
            mockProvider(DotSeoMetaTagsUtilService, {
                getMetaTags: jest.fn().mockReturnValue(mockOgTags)
            }),
            mockProvider(InlineEditService, {
                injectInlineEdit: jest.fn(),
                removeInlineEdit: jest.fn()
            }),
            {
                provide: UVEStore,
                useFactory: () => ({
                    $pageRender: pageRenderSignal,
                    editorEnableInlineEdit: editorEnableInlineEditSignal,
                    pageType: pageTypeSignal,
                    $isEmaLegacyScriptInjectionEnabled: legacyScriptInjectionEnabledSignal,
                    setSeoData: jest.fn()
                })
            }
        ]
    });

    beforeEach(() => {
        pageTypeSignal = signal(PageType.HEADLESS);
        pageRenderSignal = signal(mockPageRender);
        editorEnableInlineEditSignal = signal(false);
        legacyScriptInjectionEnabledSignal = signal(false);

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

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Component Creation', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });

        it('should set inputs correctly', () => {
            expect(component.src()).toBe('https://example.com/test');
            expect(component.title()).toBe('Test Iframe');
            expect(component.pointerEvents()).toBe('auto');
            expect(component.opacity()).toBe(1);
            expect(component.host()).toBe('*');
        });
    });

    describe('Iframe Element', () => {
        it('should render iframe with correct attributes', () => {
            const iframe = spectator.query(byTestId('iframe')) as HTMLIFrameElement;
            expect(iframe).toBeTruthy();
            expect(iframe.getAttribute('title')).toBe('Test Iframe');
            expect(iframe.getAttribute('sandbox')).toBe(
                'allow-scripts allow-same-origin allow-forms'
            );
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
            pageTypeSignal.set(PageType.HEADLESS);
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
            pageTypeSignal.set(PageType.TRADITIONAL);

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

        it('should set srcdoc to page content on iframe load', () => {
            component.onIframeLoad();
            expect(mockIframe.srcdoc).toBe(mockPageRender);
        });

        it('should not set srcdoc if iframe element is not available', () => {
            component.iframe = undefined as any;
            component.onIframeLoad();
            expect(mockIframe.srcdoc).toBe('');
        });

        describe('legacy script injection (FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION)', () => {
            it('should inject the UVE script when the flag is enabled', () => {
                legacyScriptInjectionEnabledSignal.set(true);
                component.onIframeLoad();
                expect(mockIframe.srcdoc).toContain(
                    `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`
                );
            });

            it('should NOT inject the UVE script when the flag is disabled', () => {
                legacyScriptInjectionEnabledSignal.set(false);
                component.onIframeLoad();
                expect(mockIframe.srcdoc).not.toContain(
                    `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`
                );
            });
        });
    });

    describe('handleInlineScripts', () => {
        let mockIframe: HTMLIFrameElement;
        let mockWindow: Window;

        beforeEach(() => {
            mockIframe = document.createElement('iframe');
            mockWindow = {
                addEventListener: jest.fn(),
                removeEventListener: jest.fn()
            } as unknown as Window;

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
            expect(typeof (mockWindow.addEventListener as jest.Mock).mock.calls[0][1]).toBe(
                'function'
            );
        });

        it('should emit internalNav on click', () => {
            const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
            let clickHandler: ((e: MouseEvent) => void) | undefined;

            (mockWindow.addEventListener as jest.Mock).mockImplementation(
                (event: string, handler: (e: MouseEvent) => void) => {
                    if (event === 'click') {
                        clickHandler = handler;
                    }
                }
            );

            (component as any).handleInlineScripts(false);

            if (clickHandler) {
                const linkTarget = document.createElement('a');
                linkTarget.setAttribute('href', '/test');
                const clickEvent = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(clickEvent, 'target', {
                    value: linkTarget,
                    writable: false
                });
                clickHandler(clickEvent);
                expect(internalNavSpy).toHaveBeenCalledWith(clickEvent);
            }
        });

        it('should emit inlineEditing on click', () => {
            const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');
            let clickHandler: ((e: MouseEvent) => void) | undefined;

            (mockWindow.addEventListener as jest.Mock).mockImplementation(
                (event: string, handler: (e: MouseEvent) => void) => {
                    if (event === 'click') {
                        clickHandler = handler;
                    }
                }
            );

            (component as any).handleInlineScripts(false);

            if (clickHandler) {
                const linkTarget = document.createElement('a');
                linkTarget.setAttribute('href', '/test');
                const clickEvent = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(clickEvent, 'target', {
                    value: linkTarget,
                    writable: false
                });
                clickHandler(clickEvent);
                expect(inlineEditingSpy).toHaveBeenCalledWith(clickEvent);
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

        describe('click filter', () => {
            let clickHandler: ((e: MouseEvent) => void) | undefined;
            let doc: Document;

            beforeEach(() => {
                doc = document.implementation.createHTMLDocument();
                (mockWindow.addEventListener as jest.Mock).mockImplementation(
                    (event: string, handler: (e: MouseEvent) => void) => {
                        if (event === 'click') {
                            clickHandler = handler;
                        }
                    }
                );
                (component as any).handleInlineScripts(false);
            });

            function createClickWithTarget(element: HTMLElement): MouseEvent {
                const ev = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(ev, 'target', { value: element, writable: false });
                return ev;
            }

            it('should emit internalNav and inlineEditing when click target is inside an anchor with href', () => {
                const a = doc.createElement('a');
                a.setAttribute('href', '/some-page');
                const span = doc.createElement('span');
                a.appendChild(span);

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(span));

                expect(internalNavSpy).toHaveBeenCalledTimes(1);
                expect(inlineEditingSpy).toHaveBeenCalledTimes(1);
            });

            it('should emit internalNav and inlineEditing when click target is an anchor with href', () => {
                const a = doc.createElement('a');
                a.setAttribute('href', '/page');

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(a));

                expect(internalNavSpy).toHaveBeenCalledTimes(1);
                expect(inlineEditingSpy).toHaveBeenCalledTimes(1);
            });

            it('should emit internalNav and inlineEditing when click target has data-mode', () => {
                const div = doc.createElement('div');
                div.dataset.mode = 'edit';

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(div));

                expect(internalNavSpy).toHaveBeenCalledTimes(1);
                expect(inlineEditingSpy).toHaveBeenCalledTimes(1);
            });

            it('should emit internalNav and inlineEditing when click target is inside [data-mode]', () => {
                const wrapper = doc.createElement('div');
                wrapper.setAttribute('data-mode', 'edit');
                const inner = doc.createElement('span');
                wrapper.appendChild(inner);

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(inner));

                expect(internalNavSpy).toHaveBeenCalledTimes(1);
                expect(inlineEditingSpy).toHaveBeenCalledTimes(1);
            });

            it('should not emit when click target is a plain element (no link, no data-mode)', () => {
                const div = doc.createElement('div');

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(div));

                expect(internalNavSpy).not.toHaveBeenCalled();
                expect(inlineEditingSpy).not.toHaveBeenCalled();
            });

            it('should not emit when click target is inside an anchor without href', () => {
                const a = doc.createElement('a');
                const span = doc.createElement('span');
                a.appendChild(span);

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(span));

                expect(internalNavSpy).not.toHaveBeenCalled();
                expect(inlineEditingSpy).not.toHaveBeenCalled();
            });

            it('should not emit for hash-only anchors (browser handles same-page scroll)', () => {
                const a = doc.createElement('a');
                a.setAttribute('href', '#page-section');

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(a));

                expect(internalNavSpy).not.toHaveBeenCalled();
                expect(inlineEditingSpy).not.toHaveBeenCalled();
            });

            // Anchor links are commonly placed inside an editable contentlet area
            // (`[data-mode]`). Without this guard the OR check below would still
            // emit inlineEditing for the click and trigger a TinyMCE init.
            it('should not emit for hash-only anchors nested inside [data-mode]', () => {
                const wrapper = doc.createElement('div');
                wrapper.setAttribute('data-mode', 'edit');
                const a = doc.createElement('a');
                a.setAttribute('href', '#page-section');
                wrapper.appendChild(a);

                const internalNavSpy = jest.spyOn(component.internalNav, 'emit');
                const inlineEditingSpy = jest.spyOn(component.inlineEditing, 'emit');

                clickHandler?.(createClickWithTarget(a));

                expect(internalNavSpy).not.toHaveBeenCalled();
                expect(inlineEditingSpy).not.toHaveBeenCalled();
            });
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

        it('should set OG tags and results in store in a single update', () => {
            (component as any).setSeoData();
            expect(mockUVEStore.setSeoData).toHaveBeenCalledTimes(1);
            expect(mockUVEStore.setSeoData).toHaveBeenCalledWith({
                ogTags: mockOgTags,
                ogTagsResults: mockSeoResults
            });
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

    describe('insertPageContent – de-duplicate writes', () => {
        let mockIframe: HTMLIFrameElement;
        let mockWindow: Window;

        beforeEach(() => {
            pageTypeSignal.set(PageType.TRADITIONAL);

            mockIframe = document.createElement('iframe');
            mockWindow = {
                addEventListener: jest.fn(),
                removeEventListener: jest.fn()
            } as unknown as Window;

            Object.defineProperty(mockIframe, 'contentWindow', {
                value: mockWindow,
                writable: true
            });

            component.iframe = { nativeElement: mockIframe } as any;
        });

        it('should set srcdoc to content on the first call', () => {
            component.onIframeLoad();
            expect(mockIframe.srcdoc).toBe(mockPageRender);
        });

        it('should skip srcdoc assignment on re-entrant calls with the same src and content', () => {
            component.onIframeLoad(); // first write — srcdoc = mockPageRender
            mockIframe.srcdoc = 'SENTINEL'; // manually change to detect re-assignment
            component.onIframeLoad(); // same key — should not overwrite

            expect(mockIframe.srcdoc).toBe('SENTINEL');
        });

        it('should re-write srcdoc when content changes (real-time canvas update)', () => {
            const updatedRender = '<html><body>Updated Content</body></html>';
            component.onIframeLoad();
            pageRenderSignal.set(updatedRender);
            component.onIframeLoad();

            expect(mockIframe.srcdoc).toBe(updatedRender);
        });

        it('should re-write srcdoc when src changes (page navigation)', () => {
            component.onIframeLoad();
            spectator.setInput('src', 'https://example.com/new-page');
            component.onIframeLoad();

            // Content is the same but the key changed (different src) — re-write
            expect(mockIframe.srcdoc).toBe(mockPageRender);
        });

        it('should still call handleInlineScripts on de-duplicated calls', () => {
            const handleSpy = jest.spyOn(component as any, 'handleInlineScripts');
            component.onIframeLoad();
            component.onIframeLoad();

            // handleInlineScripts must run on every call, not just srcdoc writes
            expect(handleSpy).toHaveBeenCalledTimes(2);
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
        });

        it('should insert page content when pageType changes to TRADITIONAL', () => {
            const insertSpy = jest.spyOn(component as any, 'insertPageContent');
            pageTypeSignal.set(PageType.TRADITIONAL);
            spectator.detectChanges();
            expect(insertSpy).toHaveBeenCalled();
        });

        it('should not insert page content when pageType is HEADLESS', () => {
            const insertSpy = jest.spyOn(component as any, 'insertPageContent');
            spectator.detectChanges();
            expect(insertSpy).not.toHaveBeenCalled();
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
