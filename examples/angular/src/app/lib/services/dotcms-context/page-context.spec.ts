import { TestBed } from '@angular/core/testing';

import { PageContextService } from './page-context.service';

describe('DotcmsContextService', () => {
  let service: PageContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PageContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
