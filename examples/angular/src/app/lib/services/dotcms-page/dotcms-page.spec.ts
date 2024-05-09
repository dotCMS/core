import { TestBed } from '@angular/core/testing';

import { DotcmsPageService } from './dotcms-page.service';

describe('DotcmsPageService', () => {
  let service: DotcmsPageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DotcmsPageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
