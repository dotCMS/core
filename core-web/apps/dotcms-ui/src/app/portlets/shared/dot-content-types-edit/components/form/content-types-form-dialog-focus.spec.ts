import { mockProvider } from '@openng/spectator/jest';
import { Observable, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { Dialog, DialogModule } from 'primeng/dialog';

import {
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    DotSiteService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import {
    cleanUpDialog,
    createFakeSite,
    dotcmsContentTypeBasicMock,
    DotWorkflowServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { ContentTypesFormComponent } from './content-types-form.component';

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(false);
    }
}

const messageServiceMock = new MockDotMessageService({
    'contenttypes.content.content': 'Content',
    'contenttypes.content.widget': 'Widget',
    'contenttypes.form.name': 'Name',
    'contenttypes.form.label.icon': 'Icon',
    'contenttypes.form.label.description': 'Description',
    'contenttypes.form.field.host_folder.label': 'Host or Folder',
    'contenttypes.form.label.workflow': 'Workflow',
    'contenttypes.form.label.workflow.actions': 'Workflow Actions',
    'contenttypes.form.label.publish.date.field': 'Publish Date Field',
    'contenttypes.form.field.expire.date.field': 'Expire Date Field',
    'contenttypes.form.field.detail.page': 'Detail Page',
    'contenttypes.form.label.URL.pattern': 'URL Pattern',
    'content.type.form.banner.message': 'Try the new content editor'
});

/** Fresh route per test so no shared mutable flag state leaks between them. */
const buildActivatedRoute = (newContentEditorEnabled: boolean) => ({
    snapshot: {
        data: {
            featuredFlags: {
                [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: newContentEditorEnabled
            }
        }
    }
});

const fakeSite = createFakeSite({ hostname: 'demo.dotcms.com', archived: false });

/** Mirrors the create-mode markup: the real form rendered inside a real PrimeNG dialog. */
@Component({
    selector: 'dot-test-dialog-focus-host',
    template: `
        <p-dialog [visible]="true" [modal]="true" [focusOnShow]="focusOnShow">
            <dot-content-types-form [contentType]="contentType" />
        </p-dialog>
    `,
    imports: [DialogModule, ContentTypesFormComponent]
})
class DialogFocusHostComponent {
    focusOnShow = false;
    contentType: DotCMSContentType = {
        ...dotcmsContentTypeBasicMock,
        baseType: 'CONTENT'
    };
}

const NAME_INPUT_SELECTOR = '#content-type-form-name';
const NEW_EDIT_CONTENT_CHECKBOX_SELECTOR = '#newEditContentLabel';

describe('ContentTypesFormComponent focus inside p-dialog', () => {
    let fixture: ComponentFixture<DialogFocusHostComponent>;
    let originalOffsetParent: PropertyDescriptor;

    const queryElement = (selector: string): HTMLElement =>
        fixture.debugElement.query(By.css(selector))?.nativeElement ?? null;

    const formComponent = (): ContentTypesFormComponent =>
        fixture.debugElement.query(By.directive(ContentTypesFormComponent))
            .componentInstance as ContentTypesFormComponent;

    /**
     * Renders the dialog and settles focus.
     *
     * jsdom never fires a CSS transition end, so PrimeNG's `onAfterEnter` is invoked directly to
     * reproduce its post-animation focus call. Both contenders are one-shot timers pending at that
     * point (dotAutofocus at 100ms, PrimeNG at its transition duration), so flushing pending timers
     * runs them in their real order without coupling the test to either vendor's delay.
     */
    const openDialogAndSettleFocus = ({
        focusOnShow,
        newContentEditorEnabled,
        baseType = 'CONTENT'
    }: {
        focusOnShow: boolean;
        newContentEditorEnabled: boolean;
        baseType?: DotCMSBaseTypesContentTypes;
    }): void => {
        TestBed.configureTestingModule({
            imports: [DialogFocusHostComponent],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotWorkflowService, useClass: DotWorkflowServiceMock },
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                {
                    provide: ActivatedRoute,
                    useValue: buildActivatedRoute(newContentEditorEnabled)
                },
                mockProvider(DotSiteService, {
                    getSites: jest.fn().mockReturnValue(
                        of({
                            sites: [fakeSite],
                            pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                        })
                    ),
                    getSiteById: jest.fn().mockReturnValue(of(fakeSite))
                }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });

        fixture = TestBed.createComponent(DialogFocusHostComponent);
        fixture.componentInstance.focusOnShow = focusOnShow;
        fixture.componentInstance.contentType = {
            ...dotcmsContentTypeBasicMock,
            baseType
        };
        fixture.detectChanges();

        const dialog: Dialog = fixture.debugElement.query(By.directive(Dialog)).componentInstance;
        dialog.onAfterEnter();

        jest.runOnlyPendingTimers();
    };

    beforeAll(() => {
        // PrimeNG only focuses elements it considers visible (DomHandler.isVisible checks
        // offsetParent). jsdom has no layout engine, so without this shim every element looks
        // hidden, PrimeNG's focus() becomes a no-op and the focus race under test never happens.
        originalOffsetParent = Object.getOwnPropertyDescriptor(
            HTMLElement.prototype,
            'offsetParent'
        );
        Object.defineProperty(HTMLElement.prototype, 'offsetParent', {
            configurable: true,
            get(): Element | null {
                return this.parentElement;
            }
        });
    });

    afterAll(() => {
        Object.defineProperty(HTMLElement.prototype, 'offsetParent', originalOffsetParent);
    });

    beforeEach(() => {
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
        cleanUpDialog(fixture);
    });

    describe('focusOnShow disabled (create mode)', () => {
        // The binding does not branch on baseType, but every base type reaches this same dialog
        // through create/:type — so the focus outcome is asserted for real, not just inferred.
        it.each<DotCMSBaseTypesContentTypes>(['CONTENT', 'WIDGET'])(
            'should focus the name input instead of the new content banner checkbox for %s',
            (baseType) => {
                openDialogAndSettleFocus({
                    focusOnShow: false,
                    newContentEditorEnabled: true,
                    baseType
                });

                const nameInput = queryElement(NAME_INPUT_SELECTOR);
                const newEditContentCheckbox = queryElement(NEW_EDIT_CONTENT_CHECKBOX_SELECTOR);

                expect(newEditContentCheckbox).not.toBeNull();
                expect(document.activeElement).toBe(nameInput);
                expect(document.activeElement).not.toBe(newEditContentCheckbox);
            }
        );

        it('should focus the name input when the new content banner is hidden', () => {
            openDialogAndSettleFocus({ focusOnShow: false, newContentEditorEnabled: false });

            expect(queryElement(NEW_EDIT_CONTENT_CHECKBOX_SELECTOR)).toBeNull();
            expect(document.activeElement).toBe(queryElement(NAME_INPUT_SELECTOR));
        });

        it('should let the focused input update the name control without touching the banner', () => {
            openDialogAndSettleFocus({ focusOnShow: false, newContentEditorEnabled: true });

            const form = formComponent().form;
            const newEditContentBefore = form.get('newEditContent').value;
            const nameInput = document.activeElement as HTMLInputElement;

            nameInput.value = 'My Content Type';
            nameInput.dispatchEvent(new Event('input'));

            expect(form.get('name').value).toBe('My Content Type');
            expect(form.get('newEditContent').value).toBe(newEditContentBefore);
        });
    });

    describe('focusOnShow enabled (edit mode)', () => {
        it('should focus the first focusable element in DOM order', () => {
            // Documents the current, intentionally untouched edit-mode behavior: PrimeNG wins the
            // race and focuses the DOM-first focusable element instead of the name input.
            openDialogAndSettleFocus({ focusOnShow: true, newContentEditorEnabled: true });

            expect(document.activeElement).toBe(queryElement(NEW_EDIT_CONTENT_CHECKBOX_SELECTOR));
        });
    });
});
