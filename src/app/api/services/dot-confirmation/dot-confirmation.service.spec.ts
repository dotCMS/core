import { DotConfirmationService } from './dot-confirmation.service';
import { DOTTestBed } from '../../../test/dot-test-bed';

describe('DotConfirmationService', () => {
    let mockData;
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            DotConfirmationService
        ]);

        mockData = {
            footerLabel: {
                acceptLabel: 'Delete',
                rejectLabel: 'Reject'
            }
        };

        this.dotConfirmationService =  this.injector.get(DotConfirmationService);
    });

    it('should emit data to labels property', () => {
        this.dotConfirmationService.confirm(mockData);

        this.dotConfirmationService.labels.subscribe((message) => {
            expect(message).toEqual({
                acceptLabel: 'Delete',
                rejectLabel: 'Reject'
            });
        });
    });

    it('should call confirmation service with data parameter', () => {
        spyOn(this.dotConfirmationService.confirmationService, 'confirm');

        this.dotConfirmationService.confirm(mockData);

        expect(this.dotConfirmationService.confirmationService.confirm).toHaveBeenCalledWith(mockData);
    });
});

