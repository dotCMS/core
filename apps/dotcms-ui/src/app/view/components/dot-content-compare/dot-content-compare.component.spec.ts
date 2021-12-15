import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentCompareComponent } from './dot-content-compare.component';

describe('DotContentCompareComponent', () => {
  let component: DotContentCompareComponent;
  let fixture: ComponentFixture<DotContentCompareComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DotContentCompareComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DotContentCompareComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
