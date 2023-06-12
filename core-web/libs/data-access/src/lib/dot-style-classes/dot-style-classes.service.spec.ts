import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotStyleClassesService } from './dot-style-classes.service';

describe('DotStyleClassesService', () => {
    let service: DotStyleClassesService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotStyleClassesService]
        });
        service = TestBed.inject(DotStyleClassesService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpTestingController.verify());

    it('should fetch style classes file', () => {
        service.getStyleClassesFromFile().subscribe();
        const req = httpTestingController.expectOne('/application/templates/classes.json');
        expect(req.request.method).toEqual('GET');
    });
});
