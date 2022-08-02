import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditSidebarComponent } from './dot-edit-sidebar.component';

describe('DotEditSidebarComponent', () => {
  let component: DotEditSidebarComponent;
  let fixture: ComponentFixture<DotEditSidebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DotEditSidebarComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DotEditSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
