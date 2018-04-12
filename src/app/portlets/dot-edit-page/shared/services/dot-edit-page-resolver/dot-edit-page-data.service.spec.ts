import { DotEditPageDataService } from './dot-edit-page-data.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { async } from '@angular/core/testing';
import { mockDotRenderedPage } from '../../../../../test/dot-rendered-page.mock';
import { DotRenderedPageState } from '../../models/dot-rendered-page-state.model';
import { mockUser } from '../../../../../test/login-service.mock';

describe('DotEditPageDataService', () => {

    let dotEditPageDataService: DotEditPageDataService;

    beforeEach(
        async(() => {
            const testbed = DOTTestBed.configureTestingModule({
                providers: [
                    DotEditPageDataService
                ],
                imports: []
            });

            dotEditPageDataService = testbed.get(DotEditPageDataService);
        })
    );

    it('should return a DotRenderedPageState valid object', () => {
        const dotRenderedPageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
        dotEditPageDataService.set(dotRenderedPageState);

        expect(dotRenderedPageState).toBe(dotEditPageDataService.getAndClean());
        expect(dotEditPageDataService.getAndClean()).toBeNull();
    });
});
