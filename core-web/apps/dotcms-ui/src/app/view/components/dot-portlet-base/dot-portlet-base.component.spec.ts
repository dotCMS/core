import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotPortletBoxComponent } from './components/dot-portlet-box/dot-portlet-box.component';
import { DotPortletToolbarComponent } from './components/dot-portlet-toolbar/dot-portlet-toolbar.component';
import { DotPortletBaseComponent } from './dot-portlet-base.component';

@Component({
    standalone: true,
    template: `
        <dot-portlet-base>
            <div>Hello World</div>
        </dot-portlet-base>
    `
})
class HostComponent {}

@Component({
    standalone: true,
    template: `
        <dot-portlet-base [boxed]="false">
            <div>Hello World</div>
        </dot-portlet-base>
    `
})
class DefaultTestHostUnboxedComponent {}

@Component({
    standalone: true,
    template: `
        <dot-portlet-base>
            <dot-portlet-toolbar></dot-portlet-toolbar>
            <div>Hello World</div>
        </dot-portlet-base>
    `
})
class DefaultTestHostWithToolbarComponent {}

@Component({
    selector: 'dot-portlet-toolbar',
    template: ``
})
class DotToolbarMockComponent {}

describe('DotPortletBaseComponent', () => {

    let spectator: SpectatorHost<DotPortletBaseComponent>;
    const createHost = createHostFactory(DotPortletBaseComponent);

    it('should render boxed content', () => {
        const template = `
        <dot-portlet-base>
            <div>Hello World</div>
        </dot-portlet-base>`;
        spectator = createHost(template);

        const box = spectator.query('dot-portlet-box');
        expect(box).toBeDefined();
        expect(box).toHaveText('Hello World');

        const toolbar = spectator.query(DotPortletToolbarComponent);
        expect(toolbar).toBeNull();
    });
    
    fit('should render unboxed content', () => {
        const template = `
        <dot-portlet-base [boxed]="false">
            <div data-testId="element">Hello World</div>
        </dot-portlet-base>`;
        spectator = createHost(template);
        spectator.detectChanges();

        const box = spectator.query('dot-portlet-box');
        expect(box).toBeNull();

        console.log('element', spectator.element);
        const divElement = spectator.query(byTestId('element'));
        expect(divElement).toHaveText('Hello World');
    });
    

    /*
    it('should render toolbar as first child', () => {
        const testFixture = TestBed.createComponent(DefaultTestHostWithToolbarComponent);
        testFixture.detectChanges();

        const content: DebugElement = testFixture.debugElement.query(By.css('dot-portlet-base'));

        expect(content.nativeNode.firstChild.nodeName).toBe('DOT-PORTLET-TOOLBAR');
    });
    */
});
