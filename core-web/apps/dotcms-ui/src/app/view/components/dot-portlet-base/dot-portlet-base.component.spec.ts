import { SpectatorHost, createHostFactory, byTestId } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';

import { DotPortletBoxComponent } from './components/dot-portlet-box/dot-portlet-box.component';
import { DotPortletToolbarComponent } from './components/dot-portlet-toolbar/dot-portlet-toolbar.component';
import { DotPortletBaseComponent } from './dot-portlet-base.component';


describe('DotPortletBaseComponent', () => {

    let hostSpectator: SpectatorHost<DotPortletBaseComponent>;
    const createHost = createHostFactory({
        component: DotPortletBaseComponent,
        declarations: [
            DotPortletBoxComponent,
            MockComponent(DotPortletToolbarComponent),
        ],
        shallow: true
    });

    describe('Boxed Content', () => {

        beforeEach(() => {
            hostSpectator = createHost(`
                <dot-portlet-base>
                    <div data-testid="content">Hello World</div>
                </dot-portlet-base>
            `);
            hostSpectator.detectChanges();
        });

        it('should render boxed content by default', () => {
            const portletBox = hostSpectator.query(DotPortletBoxComponent);
            expect(portletBox).toBeTruthy();
        });

        it('should render content inside portlet box when boxed', () => {
            const content = hostSpectator.query(byTestId('content'));
            expect(content?.textContent?.trim()).toBe('Hello World');
        });
    });

    describe('Unboxed Content', () => {
        beforeEach(() => {
            hostSpectator = createHost(`
                <dot-portlet-base [boxed]="false">
                    <div data-testid="content">Hello World</div>
                </dot-portlet-base>
            `);
            hostSpectator.detectChanges();
        });

        it('should render unboxed content when boxed is false', () => {
            const portletBox = hostSpectator.query(DotPortletBoxComponent);
            expect(portletBox).toBeFalsy();
        });
    });

    describe('Toolbar Integration', () => {
        beforeEach(() => {
            hostSpectator = createHost(`
                <dot-portlet-base>
                    <dot-portlet-toolbar data-testid="toolbar"></dot-portlet-toolbar>
                    <div data-testid="content">Hello World</div>
                </dot-portlet-base>
            `);
            hostSpectator.detectChanges();
        });

        it('should render toolbar as first child when present', () => {
            const toolbar = hostSpectator.query(DotPortletToolbarComponent);
            expect(toolbar).toBeTruthy();
        });

        it('should render boxed content by default', () => {
            const portletBox = hostSpectator.query(DotPortletBoxComponent);
            expect(portletBox).toBeTruthy();
        });

        it('should render content inside portlet box when boxed', () => {
            const content = hostSpectator.query(byTestId('content'));
            expect(content?.textContent?.trim()).toBe('Hello World');
        });
    });

     describe('Toolbar Integration', () => {
        beforeEach(() => {
            hostSpectator = createHost(`
                <dot-portlet-base>
                    <dot-portlet-toolbar data-testid="toolbar"></dot-portlet-toolbar>
                    <div data-testid="content">Hello World</div>
                </dot-portlet-base>
            `);
            hostSpectator.detectChanges();
        });

        it('should render toolbar as first child when present', () => {
            const toolbar = hostSpectator.query(DotPortletToolbarComponent);
            expect(toolbar).toBeTruthy();
        });

        it('should render boxed content by default', () => {
            const portletBox = hostSpectator.query(DotPortletBoxComponent);
            expect(portletBox).toBeTruthy();
        });

        it('should render content inside portlet box when boxed', () => {
            const content = hostSpectator.query(byTestId('content'));
            expect(content?.textContent?.trim()).toBe('Hello World');
        });
    });

    describe('DefaultTestHostComponent without toolbar', () => {

        beforeEach(() => {
            hostSpectator = createHost(`
                <dot-portlet-base>
                    <div data-testid="content">Hello World</div>
                </dot-portlet-base>
            `);
            hostSpectator.detectChanges();
        });

        it('should render boxed content', () => {
            hostSpectator.detectChanges();

            const portletBox = hostSpectator.query(DotPortletBoxComponent);
            const content = hostSpectator.query(byTestId('content'));
            const toolbar = hostSpectator.query(DotPortletToolbarComponent);

            expect(portletBox).toBeTruthy();
            expect(content).toBeTruthy();
            expect(content?.textContent?.trim()).toBe('Hello World');
            expect(toolbar).toBeFalsy();
        });
    });

    describe('DefaultTestHostUnboxedComponent', () => {

        beforeEach(() => {
            hostSpectator = createHost(`
                <dot-portlet-base [boxed]="false">
                    <div data-testid="content">Hello World</div>
                </dot-portlet-base>
            `);
            hostSpectator.detectChanges();
        });

        it('should render unboxed content', () => {
            const portletBox = hostSpectator.query(DotPortletBoxComponent);
            const content = hostSpectator.query(byTestId('content'));

            expect(portletBox).toBeFalsy();
            expect(content).toBeTruthy();
            expect(content?.textContent?.trim()).toBe('Hello World');
        });
    });

    describe('DefaultTestHostWithToolbarComponent', () => {

        beforeEach(() => {
            hostSpectator = createHost(`
                <dot-portlet-base data-testid="portlet-base">
                    <dot-portlet-toolbar data-testid="toolbar"></dot-portlet-toolbar>
                    <div data-testid="content">Hello World</div>
                </dot-portlet-base>
            `);
            hostSpectator.detectChanges();
        });

        it('should render toolbar as first child', () => {
            const portletBase = hostSpectator.query(byTestId('portlet-base'));
            const toolbar = hostSpectator.query(byTestId('toolbar'));

            expect(toolbar).toBeTruthy();
            expect(portletBase.firstElementChild).toBe(toolbar);
        });
    });

});
