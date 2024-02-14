import { describe, it, expect } from '@jest/globals';
import { Spectator, createComponentFactory, SpyObject, byTestId } from '@ngneat/spectator/jest';
import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCopyContentService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSBaseTypesContentTypes, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotCopyContentModalService, ModelCopyContentResponse } from '@dotcms/ui';
import { MockDotMessageService, dotcmsContentletMock } from '@dotcms/utils-testing';

import { DotEmaDialogComponent } from './dot-ema-dialog.component';
import { DotEmaDialogStore } from './store/dot-ema-dialog.store';

import { DotActionUrlService } from '../../services/dot-action-url/dot-action-url.service';
import { PAYLOAD_MOCK } from '../../shared/consts';
import { NG_CUSTOM_EVENTS } from '../../shared/enums';

describe('DotEmaDialogComponent', () => {
    let spectator: Spectator<DotEmaDialogComponent>;
    let component: DotEmaDialogComponent;
    let storeSpy: SpyObject<DotEmaDialogStore>;
    let dotCopyContentService: DotCopyContentService;
    let dotCopyContentModalService: DotCopyContentModalService;
    let dialogService: DialogService;

    const triggerIframeCustomEvent = (
        customEvent = {
            detail: {
                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                payload: {
                    htmlPageReferer: '/my-awesome-page'
                }
            }
        }
    ) => {
        const dialogIframe = spectator.debugElement.query(By.css('[data-testId="dialog-iframe"]'));

        spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

        dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
            new CustomEvent('ng-event', {
                ...customEvent
            })
        );
        spectator.detectChanges();
    };

    const createComponent = createComponentFactory({
        component: DotEmaDialogComponent,
        imports: [HttpClientTestingModule],
        providers: [
            DotEmaDialogStore,
            HttpClient,
            DialogService,
            DotCopyContentService,
            DotCopyContentModalService,
            {
                provide: DotActionUrlService,
                useValue: {
                    getCreateContentletUrl: jest
                        .fn()
                        .mockReturnValue(of('https://demo.dotcms.com/jsp.jsp'))
                }
            },
            {
                provide: CoreWebService,
                useValue: {
                    requestView: jest.fn().mockReturnValue(of({}))
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        storeSpy = spectator.inject(DotEmaDialogStore, true);
        dotCopyContentService = spectator.inject(DotCopyContentService, true);
        dotCopyContentModalService = spectator.inject(DotCopyContentModalService, true);
        dialogService = spectator.inject(DialogService, true);
    });

    describe('DOM', () => {
        it('should make dialog visible', () => {
            component.addContentlet(PAYLOAD_MOCK);
            spectator.detectChanges();
            expect(spectator.query(byTestId('dialog'))).not.toBeNull();
        });

        it("should make the form selector visible when it's a form", () => {
            component.addForm(PAYLOAD_MOCK);
            spectator.detectChanges();
            expect(spectator.query(byTestId('form-selector'))).not.toBeNull();
        });

        it("should make the iframe visible when it's not a form", () => {
            component.addContentlet(PAYLOAD_MOCK);
            spectator.detectChanges();
            expect(spectator.query(byTestId('dialog-iframe'))).not.toBeNull();
        });
    });

    describe('outputs', () => {
        it('should dispatch custom events', () => {
            const customEventSpy = jest.spyOn(component.action, 'emit');

            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent();

            expect(customEventSpy).toHaveBeenCalledWith({
                event: expect.objectContaining({
                    isTrusted: false
                }),
                payload: PAYLOAD_MOCK
            });
        });
    });

    describe('component methods', () => {
        it('should trigger addContentlet in the store', () => {
            const addContentletSpy = jest.spyOn(storeSpy, 'addContentlet');

            component.addContentlet(PAYLOAD_MOCK);

            expect(addContentletSpy).toHaveBeenCalledWith({
                containerId: PAYLOAD_MOCK.container.identifier,
                acceptTypes: PAYLOAD_MOCK.container.acceptTypes,
                language_id: PAYLOAD_MOCK.language_id,
                payload: PAYLOAD_MOCK
            });
        });

        it('should trigger addFormContentlet in the store', () => {
            const addFormContentletSpy = jest.spyOn(storeSpy, 'addFormContentlet');

            component.addForm(PAYLOAD_MOCK);

            expect(addFormContentletSpy).toHaveBeenCalledWith(PAYLOAD_MOCK);
        });

        it('should trigger addContentletSpy in the store for widget', () => {
            const addContentletSpy = jest.spyOn(storeSpy, 'addContentlet');

            component.addWidget(PAYLOAD_MOCK);

            expect(addContentletSpy).toHaveBeenCalledWith({
                containerId: PAYLOAD_MOCK.container.identifier,
                acceptTypes: DotCMSBaseTypesContentTypes.WIDGET,
                language_id: PAYLOAD_MOCK.language_id,
                payload: PAYLOAD_MOCK
            });
        });
        it('should trigger editContentlet in the store', () => {
            const editContentletSpy = jest.spyOn(storeSpy, 'editContentlet');

            component.editContentlet(PAYLOAD_MOCK);

            expect(editContentletSpy).toHaveBeenCalledWith({
                inode: PAYLOAD_MOCK.contentlet.inode,
                title: PAYLOAD_MOCK.contentlet.title
            });
        });
        it('should trigger createContentlet in the store', () => {
            const createContentletSpy = jest.spyOn(storeSpy, 'createContentlet');

            component.createContentlet({
                url: 'https://demo.dotcms.com/jsp.jsp',
                contentType: 'test'
            });

            expect(createContentletSpy).toHaveBeenCalledWith({
                contentType: 'test',
                url: 'https://demo.dotcms.com/jsp.jsp'
            });
        });

        it('should trigger createContentletFromPalette in the store', () => {
            const createContentletFromPalletSpy = jest.spyOn(
                storeSpy,
                'createContentletFromPalette'
            );

            component.createContentletFromPalette({
                variable: 'test',
                name: 'test',
                payload: PAYLOAD_MOCK
            });

            expect(createContentletFromPalletSpy).toHaveBeenCalledWith({
                name: 'test',
                payload: PAYLOAD_MOCK,
                variable: 'test'
            });
        });

        it('should trigger a reset in the store', () => {
            const resetSpy = jest.spyOn(storeSpy, 'resetDialog');

            component.resetDialog();

            expect(resetSpy).toHaveBeenCalled();
        });

        describe('Copy content', () => {
            const treeNodeMock = {
                containerId: '123',
                contentId: '123',
                pageId: '123',
                relationType: 'test',
                treeOrder: '1',
                variantId: 'test',
                personalization: 'dot:default'
            };
            const newContentlet = {
                ...dotcmsContentletMock,
                inode: '123',
                title: 'test'
            };

            let modalSpy: jest.SpyInstance<Observable<ModelCopyContentResponse>>;
            let copySpy: jest.SpyInstance<Observable<DotCMSContentlet>>;
            let editContentletSpy: jest.SpyInstance;
            let loadingSpy: jest.SpyInstance;
            let dialogServiceSpy: jest.SpyInstance;

            const PAYLOAD_MOCK_WITH_TREE_NODE = {
                ...PAYLOAD_MOCK,
                isInMultiplePages: true,
                treeNode: treeNodeMock
            };

            beforeEach(() => {
                editContentletSpy = jest.spyOn(storeSpy, 'editContentlet');
                loadingSpy = jest.spyOn(spectator.component.loading, 'emit');
                dialogServiceSpy = jest.spyOn(dialogService, 'open');
                modalSpy = jest.spyOn(dotCopyContentModalService, 'open');
                copySpy = jest
                    .spyOn(dotCopyContentService, 'copyInPage')
                    .mockReturnValue(of(newContentlet));
            });

            it('should copy and trigger editContentlet in the store', () => {
                dialogServiceSpy.mockReturnValue({
                    onClose: of('Copy')
                });
                component.editContentlet(PAYLOAD_MOCK_WITH_TREE_NODE);

                expect(editContentletSpy).toHaveBeenCalledWith({
                    inode: newContentlet.inode,
                    title: newContentlet.title
                });

                expect(modalSpy).toHaveBeenCalled();
                expect(copySpy).toHaveBeenCalledWith(treeNodeMock);
                expect(loadingSpy).toHaveBeenNthCalledWith(1, true);
                expect(loadingSpy).toHaveBeenNthCalledWith(2, false);
            });

            it('should not copy and trigger editContentlet in the store', () => {
                dialogServiceSpy.mockReturnValue({
                    onClose: of('NotCopy')
                });

                component.editContentlet(PAYLOAD_MOCK_WITH_TREE_NODE);

                expect(editContentletSpy).toHaveBeenCalledWith({
                    inode: PAYLOAD_MOCK_WITH_TREE_NODE.contentlet.inode,
                    title: PAYLOAD_MOCK_WITH_TREE_NODE.contentlet.title
                });

                expect(modalSpy).toHaveBeenCalled();
                expect(copySpy).not.toHaveBeenCalledWith();
                expect(loadingSpy).toHaveBeenNthCalledWith(1, true);
                expect(loadingSpy).toHaveBeenNthCalledWith(2, false);
            });
        });
    });
});
