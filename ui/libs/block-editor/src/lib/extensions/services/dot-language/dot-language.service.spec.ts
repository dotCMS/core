import { TestBed } from '@angular/core/testing';

import { DotLanguageService } from './dot-language.service';

describe('DotLanguageService', () => {
  let service: DotLanguageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DotLanguageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
