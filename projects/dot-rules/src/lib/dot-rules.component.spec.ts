import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotRulesComponent } from './dot-rules.component';

describe('DotRulesComponent', () => {
  let component: DotRulesComponent;
  let fixture: ComponentFixture<DotRulesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DotRulesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DotRulesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
