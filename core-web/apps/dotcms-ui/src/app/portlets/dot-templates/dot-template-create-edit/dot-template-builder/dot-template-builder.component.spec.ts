/* eslint-disable @typescript-eslint/no-explicit-any */
import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { Subject } from 'rxjs';

import { NO_ERRORS_SCHEMA, Component, EventEmitter, Input, Output } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { TemplateBuilderComponent } from '@dotcms/template-builder';

import {
    AUTOSAVE_DEBOUNCE_TIME,
    DotTemplateBuilderComponent
} from './dot-template-builder.component';

import { DotGlobalMessageComponent } from '../../../../view/components/_common/dot-global-message/dot-global-message.component';
import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';
import { DotTemplateAdvancedComponent } from '../dot-template-advanced/dot-template-advanced.component';
import { DotTemplateItem, DotTemplateItemDesign } from '../store/dot-template.store';

@Component({
    selector: 'dot-iframe',
    template: '',
    standalone: true
})
class MockIframeComponent {
    @Input() src: string;
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();

    iframeElement = {
        nativeElement: {
            contentWindow: {
                location: {
                    reload: jest.fn()
                }
            }
        }
    };
}

describe('DotTemplateBuilderComponent', () => {
    let spectator: Spectator<DotTemplateBuilderComponent>;
    let dotRouterService: SpyObject<DotRouterService>;
    let pageLeaveRequest$: Subject<void>;

    const createComponent = createComponentFactory({
        component: DotTemplateBuilderComponent,
        imports: [DotTemplateBuilderComponent],
        detectChanges: false,
        schemas: [NO_ERRORS_SCHEMA],
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            })
        ],
        componentImports: [
            [DotTemplateAdvancedComponent, MockComponent(DotTemplateAdvancedComponent)],
            [TemplateBuilderComponent, MockComponent(TemplateBuilderComponent)],
            [DotPortletBoxComponent, MockComponent(DotPortletBoxComponent)],
            [DotGlobalMessageComponent, MockComponent(DotGlobalMessageComponent)],
            [IframeComponent, MockIframeComponent]
        ]
    });

    const createDesignItem = (overrides: Partial<any> = {}): DotTemplateItemDesign => {
        return {
            type: 'design',
            identifier: 'template-id',
            title: 'Template Title',
            friendlyName: 'Template Friendly Name',
            theme: 'theme-id',
            layout: { body: { rows: [] } },
            containers: {},
            ...overrides
        } as DotTemplateItemDesign;
    };

    const createAdvancedItem = (overrides: Partial<any> = {}): DotTemplateItem => {
        return {
            type: 'advanced',
            identifier: 'template-id',
            title: 'Template Title',
            friendlyName: 'Template Friendly Name',
            body: '<h1>hi</h1>',
            ...overrides
        } as DotTemplateItem;
    };

    beforeEach(() => {
        pageLeaveRequest$ = new Subject<void>();

        spectator = createComponent({
            providers: [
                mockProvider(DotRouterService, {
                    forbidRouteDeactivation: jest.fn(),
                    pageLeaveRequest$
                })
            ]
        });

        dotRouterService = spectator.inject(DotRouterService);
    });

    it('should set lastTemplate when item input is set', () => {
        const item = createDesignItem();
        spectator.setInput('item', item);

        expect(spectator.component.item).toBe(item);
        expect(spectator.component.lastTemplate).toBe(item);
    });

    it('should set permissionsUrl and historyUrl on init', () => {
        const item = createDesignItem({ identifier: 'abc' });
        spectator.setInput('item', item);

        spectator.detectChanges();

        expect(spectator.component.permissionsUrl).toBe(
            '/html/templates/permissions.jsp?templateId=abc&popup=true'
        );
        expect(spectator.component.historyUrl).toBe(
            '/html/templates/push_history.jsp?templateId=abc&popup=true'
        );

        const permissionsIframeDe = spectator.debugElement.query(
            By.css('dot-iframe[data-testId="permissionsIframe"]')
        );
        const historyIframeDe = spectator.debugElement.query(
            By.css('dot-iframe[data-testId="historyIframe"]')
        );

        expect(permissionsIframeDe.componentInstance.src).toBe(
            '/html/templates/permissions.jsp?templateId=abc&popup=true'
        );
        expect(historyIframeDe.componentInstance.src).toBe(
            '/html/templates/push_history.jsp?templateId=abc&popup=true'
        );
    });

    describe('<dot-template-advanced> (dot-template-builder.component.html:18-23)', () => {
        it('should pass the correct inputs', () => {
            const item = createAdvancedItem({ body: 'ADV_BODY' });
            spectator.setInput('item', item);
            spectator.setInput('didTemplateChanged', true);

            spectator.detectChanges();

            const advancedDe = spectator.debugElement.query(By.css('dot-template-advanced'));
            expect(advancedDe).toBeTruthy();
            expect(advancedDe.componentInstance.body).toBe('ADV_BODY');
            expect(advancedDe.componentInstance.didTemplateChanged).toBe(true);
        });

        it('should forward outputs', () => {
            const item = createAdvancedItem({ body: 'ADV_BODY' });
            spectator.setInput('item', item);
            spectator.setInput('didTemplateChanged', true);

            const updateSpy = jest.spyOn(spectator.component.updateTemplate, 'emit');
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            const cancelSpy = jest.spyOn(spectator.component.cancel, 'emit');

            spectator.detectChanges();

            const updated = createAdvancedItem({ identifier: 'new-id' });
            spectator.triggerEventHandler('dot-template-advanced', 'updateTemplate', updated);
            spectator.triggerEventHandler('dot-template-advanced', 'save', updated);
            spectator.triggerEventHandler('dot-template-advanced', 'cancel', null);

            expect(updateSpy).toHaveBeenCalledWith(updated);
            expect(saveSpy).toHaveBeenCalledWith(updated);
            expect(cancelSpy).toHaveBeenCalled();
        });
    });

    describe('<dotcms-template-builder-lib> (dot-template-builder.component.html:26-37)', () => {
        it('should pass the correct inputs', () => {
            const item = createDesignItem({
                identifier: 'id-1',
                theme: 't-1',
                layout: { body: { rows: [{ foo: 'bar' }] } },
                containers: { c1: { identifier: 'container-1' } }
            });
            spectator.setInput('item', item);

            spectator.detectChanges();

            const builderDe = spectator.debugElement.query(By.css('dotcms-template-builder-lib'));
            expect(builderDe).toBeTruthy();
            expect(builderDe.componentInstance.layout).toEqual(item.layout as any);
            expect(builderDe.componentInstance.containerMap).toEqual(item.containers as any);
            expect(builderDe.componentInstance.template).toEqual({
                themeId: 't-1',
                identifier: 'id-1'
            });
        });

        it('should react to templateChange output', fakeAsync(() => {
            const item = createDesignItem({ identifier: 'id-1', theme: 't-1' });
            spectator.setInput('item', item);

            const updateSpy = jest.spyOn(spectator.component.updateTemplate, 'emit');
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');

            spectator.detectChanges();

            const reloadSpy = (spectator.component.historyIframe as any).iframeElement.nativeElement
                .contentWindow.location.reload as jest.Mock;

            const updated = createDesignItem({ identifier: 'id-2', theme: 't-2' });
            spectator.triggerEventHandler('dotcms-template-builder-lib', 'templateChange', updated);

            expect(reloadSpy).toHaveBeenCalledTimes(1);
            expect(dotRouterService.forbidRouteDeactivation).toHaveBeenCalledTimes(1);
            expect(updateSpy).toHaveBeenCalledWith(updated);

            tick(AUTOSAVE_DEBOUNCE_TIME - 1);
            expect(saveSpy).not.toHaveBeenCalled();

            tick(1);
            expect(saveSpy).toHaveBeenCalledWith(updated);
        }));
    });

    it('should save current lastTemplate when page leave is requested', fakeAsync(() => {
        const item = createDesignItem();
        spectator.setInput('item', item);

        const saveSpy = jest.spyOn(spectator.component.save, 'emit');

        spectator.detectChanges();

        const updated = createDesignItem({ identifier: 'id-2' });
        spectator.triggerEventHandler('dotcms-template-builder-lib', 'templateChange', updated);

        saveSpy.mockClear();
        pageLeaveRequest$.next();

        expect(saveSpy).toHaveBeenCalledWith(updated);
    }));
});
