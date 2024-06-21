import { TestBed } from '@angular/core/testing';

import { CategoriesService } from './categories.service';

describe('CategoriesService', () => {
    let service: CategoriesService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CategoriesService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
