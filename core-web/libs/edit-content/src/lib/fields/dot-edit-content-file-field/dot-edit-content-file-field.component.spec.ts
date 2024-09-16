import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';

import { DotMessageService } from '@dotcms/data-access';

import { DotEditContentFileFieldComponent } from './dot-edit-content-file-field.component';
import { FileFieldStore } from './store/file-field.store';

describe('DotEditContentFileFieldComponent', () => {
  let spectator: Spectator<DotEditContentFileFieldComponent>;
  const createComponent = createComponentFactory({
    component: DotEditContentFileFieldComponent,
    detectChanges: false,
    componentProviders: [
        FileFieldStore
    ],
    providers: [
        provideHttpClient(),
        mockProvider(DotMessageService)
    ]
  });

  beforeEach(() => spectator = createComponent());

  it('should be created', () => {
    expect(spectator.component).toBeTruthy();
  });
});