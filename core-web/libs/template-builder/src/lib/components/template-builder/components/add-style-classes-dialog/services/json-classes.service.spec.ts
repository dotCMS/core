import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { JsonClassesService, STYLE_CLASSES_FILE_URL } from './json-classes.service';

describe('JsonClassesService', () => {
    let injector: TestBed;
    let service: JsonClassesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [JsonClassesService]
        });

        injector = getTestBed();
        service = injector.inject(JsonClassesService);
        httpMock = injector.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should return an Observable with classes', () => {
        const expectedClasses = { classes: ['class1', 'class2', 'class3'] };
        // httpClientSpy.get.and.returnValue(of(expectedClasses));

        service.getClasses().subscribe((classes) => {
            expect(classes).toEqual(expectedClasses);
        });

        const req = httpMock.expectOne(STYLE_CLASSES_FILE_URL);
        expect(req.request.method).toBe('GET');
        req.flush(expectedClasses);
    });
});
