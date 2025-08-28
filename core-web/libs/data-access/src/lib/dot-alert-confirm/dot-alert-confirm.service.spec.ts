import { fakeAsync, tick, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { take } from 'rxjs/operators';

import { LoginService } from '@dotcms/dotcms-js';
import { DotAlertConfirm } from '@dotcms/dotcms-models';
import { LoginServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotAlertConfirmService } from './dot-alert-confirm.service';

import { DotMessageService } from '../dot-messages/dot-messages.service';

const messageServiceMock = new MockDotMessageService({
    'dot.common.dialog.accept': 'Go',
    'dot.common.dialog.reject': 'No'
});

describe('DotAlertConfirmService', () => {
    let mockData: DotAlertConfirm;
    let service: DotAlertConfirmService;
    let confirmationService: ConfirmationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAlertConfirmService,
                ConfirmationService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [RouterTestingModule]
        });

        mockData = {
            header: 'Header',
            message: 'Message',
            accept: jest.fn(),
            reject: jest.fn(),
            footerLabel: {
                accept: 'Delete',
                reject: 'Reject'
            }
        };

        service = TestBed.inject(DotAlertConfirmService);
        confirmationService = TestBed.inject(ConfirmationService);
    });

    describe('confirmation', () => {
        it('should set model and call confirm method in primeng service', fakeAsync(() => {
            jest.spyOn(confirmationService, 'confirm');
            service.confirmDialogOpened$.pipe(take(1)).subscribe((response: boolean) => {
                expect(response).toBe(true);
            });
            service.confirm(mockData);
            tick();
            expect(service.confirmModel).toEqual(mockData);
            expect(confirmationService.confirm).toHaveBeenCalledWith(mockData);
        }));

        it('should set model with default labels', () => {
            service.confirm({
                header: 'Header',
                message: 'Message'
            });
            expect(service.confirmModel).toEqual({
                header: 'Header',
                message: 'Message',
                footerLabel: {
                    accept: 'Go',
                    reject: 'No'
                }
            });
        });

        it('should clear model', () => {
            service.confirm(mockData);
            service.clearConfirm();
            expect(service.confirmModel).toEqual(null);
        });
    });

    describe('alert', () => {
        it('should set model', fakeAsync(() => {
            service.alert(mockData);
            expect(service.alertModel).toEqual(mockData);
            tick();
            service.confirmDialogOpened$.pipe(take(1)).subscribe((response: boolean) => {
                expect(response).toBe(true);
            });
        }));

        it('should set alert with default labels', () => {
            service.alert({
                header: 'Header',
                message: 'Message'
            });
            expect(service.alertModel).toEqual({
                header: 'Header',
                message: 'Message',
                footerLabel: {
                    accept: 'Go'
                }
            });
        });

        it('should exec accept function and clear model', () => {
            service.alert(mockData);
            service.alertAccept(new MouseEvent('click'));
            expect(mockData.accept).toHaveBeenCalledTimes(1);
            expect(service.alertModel).toEqual(null);
        });

        it('should exec reject function and clear model', () => {
            service.alert(mockData);
            service.alertReject(new MouseEvent('click'));
            expect(mockData.reject).toHaveBeenCalledTimes(1);
            expect(service.alertModel).toEqual(null);
        });
    });
});
