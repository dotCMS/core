/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, expect, it } from '@jest/globals';

import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { getTestBed, TestBed } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotMessageDisplayServiceMock,
    MockDotMessageService,
    MockDotRouterJestService,
    mockResponseView
} from '@dotcms/utils-testing';

import { DotHttpErrorManagerService } from './dot-http-error-manager.service';

import { DotAlertConfirmService } from '../dot-alert-confirm/dot-alert-confirm.service';
import { DotMessageDisplayService } from '../dot-message-display/dot-message-display.service';
import { DotMessageService } from '../dot-messages/dot-messages.service';
import { DotRouterService } from '../dot-router/dot-router.service';

describe('DotHttpErrorManagerService', () => {
    let service: DotHttpErrorManagerService;
    let dotRouterService: DotRouterService;
    let dotDialogService: DotAlertConfirmService;
    let result: any;
    let injector: TestBed;

    const messageServiceMock = new MockDotMessageService({
        'dot.common.http.error.403.header': '403 Header',
        'dot.common.http.error.403.message': '403 Message',
        'dot.common.http.error.500.header': '500 Header',
        'dot.common.http.error.500.message': '500 Message',
        'dot.common.http.error.403.license.message': 'license message',
        'dot.common.http.error.403.license.header': 'license header',
        'dot.common.http.error.400.header': '400 Header',
        'dot.common.http.error.400.message': '400 Message',
        'dot.common.http.error.204.header': '204 Header',
        'dot.common.http.error.204.message': '204 Message',
        'dot.common.http.error.409.header': '409 Header',
        'dot.common.http.error.409.message': '409 Message'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: DotRouterService, useValue: new MockDotRouterJestService(jest) },
                ConfirmationService,
                DotAlertConfirmService,
                DotHttpErrorManagerService
            ]
        });
        injector = getTestBed();
        service = injector.inject(DotHttpErrorManagerService);
        dotRouterService = injector.inject(DotRouterService);
        dotDialogService = injector.inject(DotAlertConfirmService);
    });

    it('should handle 401 error when user is logged in by redirecting to login', () => {
        jest.spyOn(dotDialogService, 'alert');
        jest.spyOn(dotRouterService, 'goToLogin');

        service.handle(mockResponseView(401)).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: true,
            status: 401
        });

        expect(dotDialogService.alert).not.toHaveBeenCalled();
        expect(dotRouterService.goToLogin).toHaveBeenCalledTimes(1);
    });

    it('should handle 401 error when user is not logged in and redirect to login', () => {
        jest.spyOn(dotDialogService, 'alert');
        jest.spyOn(dotRouterService, 'goToLogin');

        service.handle(mockResponseView(401)).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: true,
            status: 401
        });

        expect(dotDialogService.alert).not.toHaveBeenCalled();
        expect(dotRouterService.goToLogin).toHaveBeenCalledTimes(1);
    });

    it('should handle 403 error', () => {
        jest.spyOn(dotDialogService, 'alert');

        service.handle(mockResponseView(403)).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 403
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: '403 Message',
            header: '403 Header'
        });
    });

    it('should handle 500 error', () => {
        jest.spyOn(dotDialogService, 'alert');
        const headers = new HttpHeaders({
            error: 'error'
        });
        const mockViewResponse = mockResponseView(500, '', headers, {
            message: '500 Custom Message'
        });

        service.handle(mockViewResponse).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 500
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: '500 Custom Message',
            header: '500 Header'
        });
    });

    it('should handle license error', () => {
        jest.spyOn(dotDialogService, 'alert');
        const headers = new HttpHeaders({
            'error-key': 'dotcms.api.error.license.required'
        });

        const responseView: HttpErrorResponse = mockResponseView(403, undefined, headers);

        service.handle(responseView).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 403
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: 'license message',
            header: 'license header'
        });
    });

    it('should handle 400 error on message', () => {
        jest.spyOn(dotDialogService, 'alert');

        const responseView: HttpErrorResponse = mockResponseView(400, undefined, undefined, {
            message: 'Error'
        });

        service.handle(responseView).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 400
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: 'Error',
            header: '400 Header'
        });
    });

    it('should handle 400 error on error with header defined', () => {
        jest.spyOn(dotDialogService, 'alert');
        const CUSTOM_HEADER = 'Custom Header';
        const SERVER_MESSAGE = 'Server Error';

        const responseView: HttpErrorResponse = mockResponseView(400, undefined, undefined, {
            message: SERVER_MESSAGE,
            header: CUSTOM_HEADER
        });

        service.handle(responseView).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 400
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: SERVER_MESSAGE,
            header: CUSTOM_HEADER
        });
    });

    it('should handle 400 error on errors[0]', () => {
        jest.spyOn(dotDialogService, 'alert');

        const responseView: HttpErrorResponse = mockResponseView(400, undefined, undefined, [
            { message: 'Server Error' }
        ]);

        service.handle(responseView).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 400
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: 'Server Error',
            header: '400 Header'
        });
    });

    it('should handle 400 error on error.errors[0]', () => {
        jest.spyOn(dotDialogService, 'alert');

        const responseView: HttpErrorResponse = mockResponseView(400, undefined, undefined, {
            errors: [{ message: 'Server Error' }]
        });

        service.handle(responseView).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 400
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: 'Server Error',
            header: '400 Header'
        });
    });

    it('should handle 400 error on error.error', () => {
        jest.spyOn(dotDialogService, 'alert');

        const responseView: HttpErrorResponse = mockResponseView(400, undefined, undefined, {
            error: 'Server Error'
        });

        service.handle(responseView).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 400
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: 'Server Error',
            header: '400 Header'
        });
    });

    it('should handle 400 error and show reponse message', () => {
        jest.spyOn(dotDialogService, 'alert');

        service.handle(mockResponseView(400)).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 400
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: '400 Message',
            header: '400 Header'
        });
    });

    it('should handle 204 error', () => {
        jest.spyOn(dotDialogService, 'alert');

        service.handle(mockResponseView(204)).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 204
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: '204 Message',
            header: '204 Header'
        });
    });

    it('should handle 409 error and surface the BE message', () => {
        jest.spyOn(dotDialogService, 'alert');

        const conflict = new HttpErrorResponse({
            status: 409,
            error: {
                message: "Role 'test' has 2 child role(s) and cannot be deleted"
            }
        });

        service.handle(conflict).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 409
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: "Role 'test' has 2 child role(s) and cannot be deleted",
            header: '409 Header'
        });
    });

    it('should handle 409 error and fall back to the generic message when the body has none', () => {
        jest.spyOn(dotDialogService, 'alert');

        service.handle(new HttpErrorResponse({ status: 409, error: null })).subscribe((res) => {
            result = res;
        });

        expect(result).toEqual({
            redirected: false,
            status: 409
        });
        expect(dotDialogService.alert).toHaveBeenCalledWith({
            footerLabel: { accept: 'dot.common.dialog.accept' },
            message: '409 Message',
            header: '409 Header'
        });
    });
});
