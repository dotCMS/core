import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import { DotExperimentsService, DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    ActivatedRouteMock,
    DotLicenseServiceMock,
    getRunningExperimentMock,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotEmaInfoDisplayComponent } from './dot-ema-info-display.component';

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { DotPageApiService } from '../../../services/dot-page-api.service';
import { EDITOR_MODE } from '../../../shared/enums';

describe('DotEmaInfoDisplayComponent', () => {
    let spectator: Spectator<DotEmaInfoDisplayComponent>;

    const createComponent = createComponentFactory({
        component: DotEmaInfoDisplayComponent,
        imports: [CommonModule, HttpClientTestingModule],
        providers: [
            EditEmaStore,
            DotExperimentsService,
            MessageService,
            {
                provide: ActivatedRoute,
                useClass: ActivatedRouteMock
            },
            {
                provide: DotPageApiService,
                useValue: {
                    get: jest.fn()
                }
            },
            {
                provide: DotLicenseService,
                useValue: new DotLicenseServiceMock()
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: (key) => key
                }
            }
        ]
    });

    describe('device', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    currentExperiment: getRunningExperimentMock(),
                    editorData: {
                        mode: EDITOR_MODE.DEVICE,
                        device: { ...mockDotDevices[0], icon: 'someIcon' }
                    }
                }
            });
        });
        it('should show name, sizes, icon and action button of the selected device', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'iphone 200 x 100'
            );
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
            expect(spectator.query(byTestId('info-action'))).not.toBeNull();
        });
    });
});
