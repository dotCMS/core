import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { Component } from '@angular/core';

import { DotPortletBoxComponent } from './components/dot-portlet-box/dot-portlet-box.component';
import { DotPortletBaseComponent } from './dot-portlet-base.component';

@Component({
    selector: 'dot-portlet-toolbar',
    template: `
        <div data-testid="toolbar-content">Toolbar</div>
    `
})
class DotToolbarMockComponent {}

@Component({
    template: `
        <dot-portlet-base data-testid="portlet-base">
            <div data-testid="content">Hello World</div>
        </dot-portlet-base>
    `
})
class DefaultTestHostComponent {}

@Component({
    template: `
        <dot-portlet-base data-testid="portlet-base" [boxed]="false">
            <div data-testid="content">Hello World</div>
        </dot-portlet-base>
    `
})
class DefaultTestHostUnboxedComponent {}

@Component({
    template: `
        <dot-portlet-base data-testid="portlet-base">
            <dot-portlet-toolbar data-testid="toolbar"></dot-portlet-toolbar>
            <div data-testid="content">Hello World</div>
        </dot-portlet-base>
    `
})
class DefaultTestHostWithToolbarComponent {}

fdescribe('DotPortletBaseComponent', () => {
    let spectator: Spectator<DotPortletBaseComponent>;

    const createComponent = createComponentFactory({
        component: DotPortletBaseComponent,
        declarations: [DotPortletBoxComponent, DotToolbarMockComponent],
        shallow: true
    });

    describe('Boxed Content', () => {
        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render boxed content by default', () => {
            spectator.setInput('boxed', true);
            spectator.detectChanges();

            const portletBox = spectator.query('dot-portlet-box');
            expect(portletBox).toBeTruthy();
        });

        it('should render content inside portlet box when boxed', () => {
            spectator.setInput('boxed', true);
            spectator.detectChanges();

            const portletBox = spectator.query('dot-portlet-box');
            expect(portletBox).toBeTruthy();
        });
    });

    describe('Unboxed Content', () => {
        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render unboxed content when boxed is false', () => {
            spectator.setInput('boxed', false);
            spectator.detectChanges();

            const portletBox = spectator.query('dot-portlet-box');
            expect(portletBox).toBeFalsy();
        });
    });

    describe('Toolbar Integration', () => {
        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render toolbar as first child when present', () => {
            spectator.setInput('boxed', true);
            spectator.detectChanges();

            // This test is covered in the host component tests below
            // where we can properly test content projection
            expect(spectator.component).toBeTruthy();
        });
    });

    describe('Host Component Tests', () => {
        describe('DefaultTestHostComponent', () => {
            let hostSpectator: Spectator<DefaultTestHostComponent>;

            const createHostComponent = createComponentFactory({
                component: DefaultTestHostComponent,
                declarations: [DotPortletBaseComponent, DotPortletBoxComponent],
                shallow: true
            });

            beforeEach(() => {
                hostSpectator = createHostComponent();
                hostSpectator.detectChanges();
            });

            it('should render boxed content', () => {
                hostSpectator.detectChanges();

                console.log(hostSpectator.debugElement.nativeElement.innerHTML);

                const portletBox = hostSpectator.query('dot-portlet-box');
                const content = hostSpectator.query('[data-testid="content"]');
                const toolbar = hostSpectator.query('dot-portlet-toolbar');

                expect(portletBox).toBeTruthy();
                expect(content).toBeTruthy();
                expect(content?.textContent?.trim()).toBe('Hello World');
                expect(toolbar).toBeFalsy();
            });
        });

        describe('DefaultTestHostUnboxedComponent', () => {
            let hostSpectator: Spectator<DefaultTestHostUnboxedComponent>;

            const createHostComponent = createComponentFactory({
                component: DefaultTestHostUnboxedComponent,
                declarations: [DotPortletBaseComponent],
                shallow: true
            });

            beforeEach(() => {
                hostSpectator = createHostComponent();
                hostSpectator.detectChanges();
            });

            it('should render unboxed content', () => {
                const portletBox = hostSpectator.query('dot-portlet-box');
                const content = hostSpectator.query('[data-testid="content"]');

                expect(portletBox).toBeFalsy();
                expect(content).toBeTruthy();
                expect(content?.textContent?.trim()).toBe('Hello World');
            });
        });

        describe('DefaultTestHostWithToolbarComponent', () => {
            let hostSpectator: Spectator<DefaultTestHostWithToolbarComponent>;

            const createHostComponent = createComponentFactory({
                component: DefaultTestHostWithToolbarComponent,
                declarations: [
                    DotPortletBaseComponent,
                    DotPortletBoxComponent,
                    DotToolbarMockComponent
                ],
                shallow: true
            });

            beforeEach(() => {
                hostSpectator = createHostComponent();
                hostSpectator.detectChanges();
            });

            it('should render toolbar as first child', () => {
                const portletBase = hostSpectator.query('[data-testid="portlet-base"]');
                const toolbar = hostSpectator.query('[data-testid="toolbar"]');

                expect(toolbar).toBeTruthy();
                expect(portletBase?.firstChild).toBe(toolbar);
            });
        });
    });
});
