import { TestBed } from '@angular/core/testing';

import { DotRulesService } from './dot-rules.service';

describe('DotRulesService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: DotRulesService = TestBed.get(DotRulesService);
    expect(service).toBeTruthy();
  });
});
