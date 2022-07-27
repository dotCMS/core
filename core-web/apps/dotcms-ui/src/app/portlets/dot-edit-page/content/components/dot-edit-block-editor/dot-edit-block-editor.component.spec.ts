import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditBlockEditorComponent } from './dot-edit-block-editor.component';

describe('DotEditBlockEditorComponent', () => {
  let component: DotEditBlockEditorComponent;
  let fixture: ComponentFixture<DotEditBlockEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DotEditBlockEditorComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DotEditBlockEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
