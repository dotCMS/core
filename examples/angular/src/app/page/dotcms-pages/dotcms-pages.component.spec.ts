import { TestBed } from '@angular/core/testing';
import { DotCMSPagesComponent } from './dotcms-pages.component';

describe('DotCMSPagesComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DotCMSPagesComponent],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(DotCMSPagesComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
