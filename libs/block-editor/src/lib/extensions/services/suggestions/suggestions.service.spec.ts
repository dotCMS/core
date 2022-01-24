import { TestBed } from '@angular/core/testing';

import { SuggestionsService } from './suggestions.service';

describe('SuggestionsService', () => {
    let service: SuggestionsService;

    beforeEach(() => {
        TestBed.configureTestingModule({ teardown: { destroyAfterEach: false } });
        service = TestBed.inject(SuggestionsService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
