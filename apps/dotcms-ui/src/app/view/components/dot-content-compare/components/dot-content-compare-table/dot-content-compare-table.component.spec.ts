import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentCompareTableComponent } from './dot-content-compare-table.component';

describe('DotContentCompareTableComponent', () => {
  let component: DotContentCompareTableComponent;
  let fixture: ComponentFixture<DotContentCompareTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DotContentCompareTableComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DotContentCompareTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
