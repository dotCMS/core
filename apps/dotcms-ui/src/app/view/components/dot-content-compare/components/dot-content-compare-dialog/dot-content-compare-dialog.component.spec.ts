import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentCompareDialogComponent } from './dot-content-compare-dialog.component';

describe('DotContentCompareDialogComponent', () => {
  let component: DotContentCompareDialogComponent;
  let fixture: ComponentFixture<DotContentCompareDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DotContentCompareDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DotContentCompareDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
