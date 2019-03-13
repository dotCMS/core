import { TestBed } from '@angular/core/testing';

import { DotDialogMessageService } from './dot-dialog-message.service';

describe('DotDialogMessageService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: DotDialogMessageService = TestBed.get(DotDialogMessageService);
    expect(service).toBeTruthy();
  });
});
