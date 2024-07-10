import { Spectator } from '@ngneat/spectator';
import { createRoutingFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import * as dotcmsClient from '@dotcms/client';

import { PageResponseMock } from './../../utils/testing.utils';
import { DotcmsLayoutComponent } from './dotcms-layout.component';

import { DotCMSContentlet, DotCMSPageAsset } from '../../models';
import { PageContextService } from '../../services/dotcms-context/page-context.service';
import { RowComponent } from '../row/row.component';

@Component({
    selector: 'dotcms-mock-component',
    standalone: true,
    template: 'Hello world'
})
class DotcmsSDKMockComponent {
    @Input() contentlet!: DotCMSContentlet;
}

jest.mock('@dotcms/client', () => ({
    isInsideEditor: jest.fn().mockReturnValue(true),
    initEditor: jest.fn(),
    updateNavigation: jest.fn(),
    CUSTOMER_ACTIONS: {
        GET_PAGE_DATA: 'get-page-data'
    }
}));

describe('DotcmsLayoutComponent', () => {
    let spectator: Spectator<DotcmsLayoutComponent>;

    const createComponent = createRoutingFactory({
        component: DotcmsLayoutComponent,
        imports: [MockComponent(RowComponent)],
        providers: [
            { provide: ActivatedRoute, useValue: { url: of([]) } },
            { provide: Router, useValue: {} },
            {
                provide: PageContextService,
                useValue: {
                    setContext: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                pageAsset: PageResponseMock as unknown as DotCMSPageAsset,
                components: {
                    Banner: Promise.resolve(DotcmsSDKMockComponent)
                }
            }
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render rows', () => {
        spectator.detectChanges();
        expect(spectator.queryAll(RowComponent).length).toBe(3);
    });

    it('should save pageContext', () => {
        spectator.detectChanges();
        jest.spyOn(spectator.inject(PageContextService), 'setContext');
        expect(spectator.inject(PageContextService).setContext).toHaveBeenCalled();
    });

    describe('inside editor', () => {
        it('should call initEditor and updateNavigation from @dotcms/client', () => {
            const initEditorSpy = jest.spyOn(dotcmsClient, 'initEditor');
            const updateNavigationSpy = jest.spyOn(dotcmsClient, 'updateNavigation');

            spectator.detectChanges();
            expect(initEditorSpy).toHaveBeenCalled();
            expect(updateNavigationSpy).toHaveBeenCalled();
        });

        it('should listen to SET_PAGE_DATA message', () => {
            spectator.detectChanges();
            window.dispatchEvent(
                new MessageEvent('message', { data: { name: 'SET_PAGE_DATA', payload: {} } })
            );
            expect(spectator.inject(PageContextService).setContext).toHaveBeenCalled();
        });
    });
});
