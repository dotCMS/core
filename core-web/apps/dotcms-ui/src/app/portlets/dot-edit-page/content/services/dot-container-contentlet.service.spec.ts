import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotSessionStorageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotPage,
    DotPageContainer,
    DotPageContent
} from '@dotcms/dotcms-models';
import { dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import { DotContainerContentletService } from './dot-container-contentlet.service';

describe('DotContainerContentletService', () => {
    let dotContainerContentletService: DotContainerContentletService;
    let httpMock: HttpTestingController;
    let dotSessionStorageService: DotSessionStorageService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotContainerContentletService,
                DotSessionStorageService
            ]
        });
        dotContainerContentletService = TestBed.inject(DotContainerContentletService);
        httpMock = TestBed.inject(HttpTestingController);
        dotSessionStorageService = TestBed.inject(DotSessionStorageService);
    });

    it('should do a request for get the contentlet html code without variant', () => {
        // Mock the DotSessionStorageService to return undefined (no variant)
        jest.spyOn(dotSessionStorageService, 'getVariationId').mockReturnValue(undefined);

        const pageContainer: DotPageContainer = {
            identifier: '1',
            uuid: '3'
        };

        const pageContent: DotPageContent = {
            identifier: '2',
            inode: '4',
            type: 'content_type'
        };

        const dotPage: DotPage = {
            canEdit: true,
            canRead: true,
            canLock: true,
            identifier: '1',
            pageURI: '/page_test',
            shortyLive: 'shortyLive',
            shortyWorking: 'shortyWorking',
            workingInode: '2',
            contentType: undefined,
            fileAsset: false,
            friendlyName: '',
            host: '',
            inode: '2',
            name: '',
            systemHost: false,
            type: '',
            uri: '',
            versionType: ''
        };

        dotContainerContentletService
            .getContentletToContainer(pageContainer, pageContent, dotPage)
            .subscribe();
        httpMock.expectOne('/api/v1/containers/content/2?containerId=1&pageInode=2');
    });

    it('should do a request for get the form html code', () => {
        const formId = '2';
        const pageContainer: DotPageContainer = {
            identifier: '1',
            uuid: '3'
        };

        const form: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: DotCMSClazzes.TEXT,
            defaultType: true,
            fixed: true,
            folder: 'folder',
            host: 'host',
            name: 'name',
            owner: 'owner',
            system: false,
            baseType: 'form',
            id: formId
        };

        dotContainerContentletService.getFormToContainer(pageContainer, form.id).subscribe();
        httpMock.expectOne('/api/v1/containers/form/2?containerId=1');
    });

    it('should do a request for get the contentlet html code in a specific variant', () => {
        // Mock the DotSessionStorageService to return the Testing variant
        jest.spyOn(dotSessionStorageService, 'getVariationId').mockReturnValue('Testing');

        const pageContainer: DotPageContainer = {
            identifier: '1',
            uuid: '3'
        };

        const pageContent: DotPageContent = {
            identifier: '2',
            inode: '4',
            type: 'content_type'
        };

        const dotPage: DotPage = {
            canEdit: true,
            canRead: true,
            canLock: true,
            identifier: '1',
            pageURI: '/page_test',
            shortyLive: 'shortyLive',
            shortyWorking: 'shortyWorking',
            workingInode: '2',
            contentType: undefined,
            fileAsset: false,
            friendlyName: '',
            host: '',
            inode: '2',
            name: '',
            systemHost: false,
            type: '',
            uri: '',
            versionType: ''
        };

        dotContainerContentletService
            .getContentletToContainer(pageContainer, pageContent, dotPage)
            .subscribe();
        httpMock.expectOne(
            '/api/v1/containers/content/2?containerId=1&pageInode=2&variantName=Testing'
        );
    });

    afterEach(() => {
        httpMock.verify();
        jest.clearAllMocks();
    });
});
