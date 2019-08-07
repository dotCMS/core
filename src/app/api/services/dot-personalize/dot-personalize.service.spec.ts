import { TestBed } from '@angular/core/testing';

import { DotPersonalizeService } from './dot-personalize.service';

describe('DotPersonalizeService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: DotPersonalizeService = TestBed.get(DotPersonalizeService);
    expect(service).toBeTruthy();
  });
});
