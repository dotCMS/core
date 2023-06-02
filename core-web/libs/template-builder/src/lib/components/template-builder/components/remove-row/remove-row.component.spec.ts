import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { RemoveRowComponent } from './remove-row.component';

fdescribe('RemoveRowComponent', () => {
    let spectator: Spectator<RemoveRowComponent>;
    let mockConfirmationService: ConfirmationService;

    const createComponent = createComponentFactory({
        component: RemoveRowComponent,
        imports: [ConfirmPopupModule, ButtonModule, BrowserAnimationsModule],
        providers: [ConfirmationService],
        mocks: [ConfirmationService]
    });

    beforeEach(() => {
        spectator = createComponent();
        mockConfirmationService = spectator.inject(ConfirmationService);
        jest.spyOn(mockConfirmationService, 'confirm').mockImplementation(({ accept }) => accept());
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    /*  it('should emit deleteRow event when confirmed', () => {
    const emitSpy = jest.spyOn(spectator.component.deleteRow, 'emit');

    spectator.component.confirm(new MouseEvent('click'));

    expect(mockConfirmationService.confirm).toHaveBeenCalled();
    expect(emitSpy).toHaveBeenCalled();
  }); */
});
