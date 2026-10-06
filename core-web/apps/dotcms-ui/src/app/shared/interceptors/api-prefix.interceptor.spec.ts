import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiPrefixInterceptor } from './api-prefix.interceptor';

describe('apiPrefixInterceptor', () => {
    let http: HttpClient;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(withInterceptors([apiPrefixInterceptor])),
                provideHttpClientTesting()
            ]
        });
        http = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should prepend /api/ to relative versioned URL like v1/contenttype', () => {
        http.get('v1/contenttype').subscribe();
        httpMock.expectOne('/api/v1/contenttype');
    });

    it('should prepend /api/ to relative versioned URL like v2/workflow', () => {
        http.get('v2/workflow').subscribe();
        httpMock.expectOne('/api/v2/workflow');
    });

    it('should prepend /api to absolute versioned URL like /v1/contenttype', () => {
        http.get('/v1/contenttype').subscribe();
        httpMock.expectOne('/api/v1/contenttype');
    });

    it('should prepend /api to absolute versioned URL like /v3/page', () => {
        http.get('/v3/page').subscribe();
        httpMock.expectOne('/api/v3/page');
    });

    it('should not modify URL that already starts with /api/', () => {
        http.get('/api/v1/contenttype').subscribe();
        httpMock.expectOne('/api/v1/contenttype');
    });

    it('should not modify absolute http:// URLs', () => {
        http.get('http://example.com/v1/data').subscribe();
        httpMock.expectOne('http://example.com/v1/data');
    });

    it('should not modify absolute https:// URLs', () => {
        http.get('https://example.com/v1/data').subscribe();
        httpMock.expectOne('https://example.com/v1/data');
    });

    it('should not modify non-versioned relative URLs like assets/image.png', () => {
        http.get('assets/image.png').subscribe();
        httpMock.expectOne('assets/image.png');
    });

    it('should not modify non-versioned absolute URLs like /assets/image.png', () => {
        http.get('/assets/image.png').subscribe();
        httpMock.expectOne('/assets/image.png');
    });

    it('should not modify /html/ paths', () => {
        http.get('/html/something').subscribe();
        httpMock.expectOne('/html/something');
    });

    it('should not modify /dwr/ paths', () => {
        http.get('/dwr/call').subscribe();
        httpMock.expectOne('/dwr/call');
    });
});
