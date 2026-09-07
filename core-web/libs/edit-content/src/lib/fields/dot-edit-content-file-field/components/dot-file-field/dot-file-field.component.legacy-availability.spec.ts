import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAiService,
    DotMessageService,
    DotSiteService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './dot-file-field.component';

import { BINARY_FIELD_MOCK, FILE_FIELD_MOCK, IMAGE_FIELD_MOCK } from '../../../../utils/mocks';
import {
    LegacyDialogImageEditorLauncher,
    LegacyDojoImageEditorLauncher
} from '../../services/image-editor';
import { DotFileFieldUploadService } from '../../services/upload-file/upload-file.service';
import { FileFieldStore } from '../../store/file-field.store';
import { DotFileFieldPreviewComponent } from '../dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from '../dot-file-field-ui-message/dot-file-field-ui-message.component';

/**
 * Availability of the image editor in the legacy Dojo host — i.e. when the
 * Angular image-editor launcher (`IMAGE_EDITOR_LAUNCHER`) is NOT provided (the
 * token is only supplied by the new Angular Edit Content shell).
 *
 * Binary fields keep the binary inline and fall back to the legacy editor, so
 * they still expose the action. Image/File fields reference a separate dotAsset
 * and are new-Edit-Content-only, so they must NOT expose the editor here.
 *
 * Kept in a dedicated spec because Spectator allows a single
 * `createComponentFactory` per file, and this scenario needs a factory that
 * omits the launcher token.
 */
/** The AssetPicker needs a site to browse. */
const SITE_MOCK: DotSite = {
    identifier: 'site-1',
    hostname: 'demo.dotcms.com',
    aliases: null,
    archived: false
};

describe('DotFileFieldComponent — legacy host availability (no Angular launcher)', () => {
    let spectator: Spectator<DotFileFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotFileFieldComponent,
        imports: [ReactiveFormsModule],
        componentMocks: [DotFileFieldPreviewComponent, DotFileFieldUiMessageComponent],
        providers: [
            // Deliberately NO Router and NO GlobalStore: the legacy Dojo host is a custom element
            // bootstrapped without a router, so anything the component pulls in has to survive that.
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(SITE_MOCK))
            }),
            FileFieldStore,
            mockProvider(DotFileFieldUploadService),
            mockProvider(DialogService),
            LegacyDialogImageEditorLauncher,
            LegacyDojoImageEditorLauncher,
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('Test Message') }),
            mockProvider(DotAiService, {
                checkPluginInstallation: jest.fn().mockReturnValue(of(false))
            }),
            provideHttpClient(),
            provideHttpClientTesting()
            // IMAGE_EDITOR_LAUNCHER intentionally not provided (legacy host).
        ]
    });

    const setReferencedImageAsset = (field: typeof IMAGE_FIELD_MOCK) => {
        spectator = createComponent({
            props: {
                field,
                contentlet: createFakeContentlet({ [field.variable]: 'ref-identifier' }),
                hasError: false
            } as never
        });
        spectator.detectChanges();
        spectator.component.store.setPreviewFile({
            source: 'contentlet',
            file: {
                identifier: 'ref-identifier',
                assetMetaData: { isImage: true, contentType: 'image/png', name: 'beach.png' }
            } as never
        });
        spectator.detectChanges();
    };

    it('constructs in a host with no Router, as the legacy custom element has none', () => {
        // Regression: injecting `GlobalStore` here dragged in `withBreadcrumbs`, which does
        // `inject(Router)` eagerly. `dotcms-binary-field-builder` bootstraps without a router, so
        // the whole Binary Field blew up with NG0201 and rendered nothing in the Dojo editor.
        expect(() =>
            createComponent({
                props: {
                    field: BINARY_FIELD_MOCK,
                    contentlet: createFakeContentlet({ [BINARY_FIELD_MOCK.variable]: null }),
                    hasError: false
                } as never
            })
        ).not.toThrow();
    });

    it('hides the editor for an Image field even when the asset is an image', () => {
        setReferencedImageAsset(IMAGE_FIELD_MOCK);
        expect(spectator.component.$canEditImage()).toBe(false);
    });

    it('hides the editor for a File field even when the asset is an image', () => {
        setReferencedImageAsset(FILE_FIELD_MOCK);
        expect(spectator.component.$canEditImage()).toBe(false);
    });

    it('still exposes the editor for a Binary image field (legacy fallback)', () => {
        spectator = createComponent({
            props: {
                field: BINARY_FIELD_MOCK,
                contentlet: createFakeContentlet({ [BINARY_FIELD_MOCK.variable]: null }),
                hasError: false
            } as never
        });
        spectator.detectChanges();
        spectator.component.store.setPreviewFile({
            source: 'temp',
            file: {
                id: 'temp-1',
                fileName: 'img.png',
                metadata: { isImage: true, contentType: 'image/png', name: 'img.png' }
            }
        } as never);
        spectator.detectChanges();
        expect(spectator.component.$canEditImage()).toBe(true);
    });
});
