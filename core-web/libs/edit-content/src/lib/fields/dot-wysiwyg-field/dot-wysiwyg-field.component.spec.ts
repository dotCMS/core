import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotWYSIWYGFieldComponent } from './dot-wysiwyg-field.component';

describe('DotWYSIWYGFieldComponent', () => {
  let spectator: Spectator<DotWYSIWYGFieldComponent>;
  const createComponent = createComponentFactory(DotWYSIWYGFieldComponent);

  it('should create', () => {
    spectator = createComponent();

    expect(spectator.component).toBeTruthy();
  });
});
