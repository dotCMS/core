import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { DotSessionStorageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotPage,
    DotPageContainer,
    DotPageContent
} from '@dotcms/dotcms-models';
import { CoreWebServiceMock, dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import { DotContainerContentletService } from './dot-container-contentlet.service';

describe('DotContainerContentletService', () => {
    let injector: TestBed;
    let dotContainerContentletService: DotContainerContentletService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotContainerContentletService,
                DotSessionStorageService
            ]
        });
        injector = getTestBed();
        dotContainerContentletService = injector.get(DotContainerContentletService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should do a request for get the contentlet html code', () => {
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
        httpMock.expectOne(`v1/containers/content/2?containerId=1&pageInode=2&variantName=DEFAULT`);
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
        httpMock.expectOne(`v1/containers/form/2?containerId=1`);
    });

    it('should do a request for get the contentlet html code in a specific variant', () => {
        window.sessionStorage.setItem('variantName', 'Testing');

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
        httpMock.expectOne(`v1/containers/content/2?containerId=1&pageInode=2&variantName=Testing`);
    });

    afterEach(() => {
        httpMock.verify();
        window.sessionStorage.removeItem('variantName');
    });
});
