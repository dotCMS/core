import { TestBed } from '@angular/core/testing';
import { DotContentCompareStore } from '@components/dot-content-compare/store/dot-content-compare.store';

describe('DotContentCompare.Store.TsService', () => {
    let service: DotContentCompareStore;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotContentCompareStore);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
