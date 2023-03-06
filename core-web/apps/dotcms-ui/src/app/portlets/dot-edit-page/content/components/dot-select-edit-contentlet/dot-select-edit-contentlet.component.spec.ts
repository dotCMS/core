import { of, throwError } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotCopyContentService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { MockDotMessageService, dotcmsContentletMock } from '@dotcms/utils-testing';

import {
    DotSelectEditContentletComponent,
    CONTENTLET_EDIT_MODE
} from './dot-select-edit-contentlet.component';

const messageServiceMock = new MockDotMessageService({
    'editpage.content.contentlet.several.pages': '',
    'editpage.content.contentlet.edit.in.page.or.all': '',
    'editpage.content.contentlet.edit.this.page': 'This Page',
    'editpage.content.contentlet.edit.all.pages': 'All Pages',
    'dot.common.dialog.reject': 'Reject',
    'dot.common.dialog.accept': 'Accept'
});

const DATA_MOCK = {
    inode: '3053bb65-a1a4-4c8a-9a39-b2570a04ddc3',
    copyContent: {
        pageId: '1137c7e15b3f8cbad6b401ec77e030cd',
        treeOrder: '0',
        containerId: '//demo.dotcms.com/application/containers/default/',
        contentId: '6e1673e95ed62c3e7f78813bb6707c48',
        relationType: '1562770692396',
        variantId: 'DEFAULT',
        personalization: 'dot:default'
    }
};

const CONTENTLET_MOCK: DotCMSContentlet = {
    ...dotcmsContentletMock,
    inode: '1234567890'
};

describe('DotSelectEditContentletComponent', () => {
    let component: DotSelectEditContentletComponent;
    let fixture: ComponentFixture<DotSelectEditContentletComponent>;
    let de: DebugElement;

    /**
     * let dynamicDialogConfig: DynamicDialogConfig;
     */
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotCopyContentService: DotCopyContentService;
    let dynamicDialogRef: DynamicDialogRef;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotSelectEditContentletComponent, DotMessagePipeModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle() {
                            return of({});
                        }
                    }
                },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: () => {
                            /** */
                        }
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: DATA_MOCK
                    }
                },
                {
                    provide: DotCopyContentService,
                    useValue: {
                        copyContentInPage: of({})
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotSelectEditContentletComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        /**
         * dynamicDialogConfig = TestBed.inject(DynamicDialogConfig);
         */
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        dotCopyContentService = TestBed.inject(DotCopyContentService);
        dynamicDialogRef = TestBed.inject(DynamicDialogRef);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('changeMode', () => {
        it('should change mode', () => {
            component.changeMode(CONTENTLET_EDIT_MODE.ALL);
            expect(component.mode).toBe(CONTENTLET_EDIT_MODE.ALL);
        });

        it('should change mode wehn click on dialog__container__options item', () => {
            const div = de.query(By.css(`[data-testid="${CONTENTLET_EDIT_MODE.ALL}"]`));
            div.triggerEventHandler('click', null);
            expect(component.mode).toBe(CONTENTLET_EDIT_MODE.ALL);
        });
    });

    describe('On Cancel', () => {
        it('should close the dialog without emiting indo', () => {
            spyOn(dynamicDialogRef, 'close');
            const button = de.query(By.css(`[data-testid="cancel"]`));
            button.triggerEventHandler('click', null);

            expect(dynamicDialogRef.close).toHaveBeenCalledOnceWith({});
        });
    });

    describe('Edit Master Contentlet', () => {
        it('should change close the dialog and emit the current inode', () => {
            component.changeMode(CONTENTLET_EDIT_MODE.ALL);

            spyOn(dynamicDialogRef, 'close');

            const button = de.query(By.css(`[data-testid="accept"]`));
            button.triggerEventHandler('click', null);

            expect(dynamicDialogRef.close).toHaveBeenCalledOnceWith({ inode: DATA_MOCK.inode });
        });
    });

    describe('Edit Page Contentlet', () => {
        it('should change close the dialog, make a copy of the contentlet, and emit the new inode', () => {
            component.changeMode(CONTENTLET_EDIT_MODE.CURRENT);

            spyOn(dynamicDialogRef, 'close');
            spyOn(dotCopyContentService, 'copyContentInPage').and.returnValue(of(CONTENTLET_MOCK));

            const button = de.query(By.css(`[data-testid="accept"]`));
            button.triggerEventHandler('click', null);

            expect(dynamicDialogRef.close).toHaveBeenCalledOnceWith({
                inode: CONTENTLET_MOCK.inode
            });
        });

        it('should handler error when copyContentInPage fails', () => {
            component.changeMode(CONTENTLET_EDIT_MODE.CURRENT);

            spyOn(dynamicDialogRef, 'close');
            spyOn(dotCopyContentService, 'copyContentInPage').and.returnValue(throwError({}));
            spyOn(dotHttpErrorManagerService, 'handle');

            const button = de.query(By.css(`[data-testid="accept"]`));
            button.triggerEventHandler('click', null);

            expect(dynamicDialogRef.close).not.toHaveBeenCalledWith({
                inode: CONTENTLET_MOCK.inode
            });
            expect(dynamicDialogRef.close).toHaveBeenCalledWith({});
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
        });
    });
});
