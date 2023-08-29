import { TestBed } from '@angular/core/testing';

import { JsonClassesService } from './json-classes.service';

describe('JsonClassesService', () => {
    let service: JsonClassesService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(JsonClassesService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
