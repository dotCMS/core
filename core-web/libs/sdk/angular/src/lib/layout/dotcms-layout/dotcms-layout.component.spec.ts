import { expect } from '@jest/globals';
import { Spectator, createRoutingFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import * as dotcmsClient from '@dotcms/client';

import { PageResponseMock, PageResponseOneRowMock } from './../../utils/testing.utils';
import { DotcmsLayoutComponent } from './dotcms-layout.component';

import { DotCMSContentlet, DotCMSPageAsset } from '../../models';
import { PageContextService } from '../../services/dotcms-context/page-context.service';
import { RowComponent } from '../row/row.component';

interface Callback {
    [key: string]: (data: unknown) => void;
}

interface DotCmsClientMock extends dotcmsClient.DotCmsClient {
    editor: {
        on: (type: string, callbackFn: (data: unknown) => void) => void;
        off: jest.Mock;
        callbacks: Callback;
    };
}

@Component({
    selector: 'dotcms-mock-component',
    standalone: true,
    template: 'Hello world'
})
class DotcmsSDKMockComponent {
    @Input() contentlet!: DotCMSContentlet;
}

jest.mock('@dotcms/client', () => ({
    ...jest.requireActual('@dotcms/client'),
    isInsideEditor: jest.fn().mockReturnValue(true),
    initEditor: jest.fn(),
    updateNavigation: jest.fn(),
    postMessageToEditor: jest.fn(),
    DotCmsClient: {
        instance: {
            editor: {
                on: function (type: string, callbackFn: (data: unknown) => void): void {
                    this.callbacks[type] = callbackFn;
                },
                off: jest.fn(),
                callbacks: {} as Callback
            }
        }
    },
    CUSTOMER_ACTIONS: {
        GET_PAGE_DATA: 'get-page-data'
    }
}));

const { DotCmsClient } = dotcmsClient as jest.Mocked<typeof dotcmsClient>;

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
            },
            detectChanges: false
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

        describe('onReload', () => {
            const client = DotCmsClient.instance;
            let editorOnSpy: jest.SpyInstance;

            beforeEach(() => {
                editorOnSpy = jest.spyOn(client.editor, 'on');
                spectator.setInput('onReload', () => {
                    /* do nothing */
                });
                spectator.detectChanges();
            });

            it('should subscribe to the `CHANGE` event', () => {
                expect(editorOnSpy).toHaveBeenCalled();
            });

            it('should remove listener on unmount', () => {
                spectator.component.ngOnDestroy();
                spectator.detectChanges();

                expect(client.editor.off).toHaveBeenCalledWith('changes');
            });
        });

        describe('client is ready', () => {
            const query = { query: 'query { ... }' };

            beforeEach(() => {
                spectator.setInput('editor', query);
                spectator.detectChanges();
            });

            it('should post message to editor', () => {
                spectator.detectChanges();
                expect(dotcmsClient.postMessageToEditor).toHaveBeenCalledWith({
                    action: dotcmsClient.CUSTOMER_ACTIONS.CLIENT_READY,
                    payload: query
                });
            });
        });

        describe('onChange', () => {
            const client = DotCmsClient.instance;
            beforeEach(() => spectator.detectChanges());

            it('should update the page asset when changes are made in the editor', () => {
                const editorOnSpy = jest.spyOn(client.editor, 'on');
                expect(editorOnSpy).toHaveBeenCalledWith('changes', expect.any(Function));
            });
        });
    });

    describe('template', () => {
        beforeEach(() => spectator.detectChanges());

        it('should render rows', () => {
            expect(spectator.queryAll(RowComponent).length).toBe(3);
        });

        it('should pass the correct row to RowComponent', () => {
            const rowComponents = spectator.queryAll(RowComponent);
            const rows = PageResponseMock.layout.body.rows;
            expect(rowComponents.length).toBe(rows.length);
            expect(rowComponents[0].row).toEqual(rows[0]);
            expect(rowComponents[1].row).toEqual(rows[1]);
            expect(rowComponents[2].row).toEqual(rows[2]);
        });

        it('should update the page asset when changes are made in the editor', () => {
            const { editor } = DotCmsClient.instance as DotCmsClientMock;
            editor.callbacks['changes'](PageResponseOneRowMock);
            spectator.detectChanges();
            const rowComponents = spectator.queryAll(RowComponent);
            const rows = PageResponseMock.layout.body.rows;
            expect(rowComponents.length).toBe(1);
            expect(rowComponents[0].row).toEqual(rows[0]);
        });
    });
});
