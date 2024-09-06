import { Server } from 'mock-socket';

import { WebSocketProtocol } from './websockets-protocol';

import { LoggerService } from '../logger.service';
import { StringUtils } from '../string-utils.service';

describe('WebSocketProtocol', () => {
    let webSocketProtocol: WebSocketProtocol;
    const url = 'wss://testing';
    let mockServer: Server;

    beforeEach(() => {
        const loggerService = new LoggerService(new StringUtils());
        webSocketProtocol = new WebSocketProtocol(url, loggerService);
    });

    beforeEach(() => {
        mockServer = new Server(url);
    });

    it('should connect and tigger open event', (done) => {
        webSocketProtocol.open$().subscribe(() => {
            done();
        });

        webSocketProtocol.connect();
    });

    it('should tigger message event', (done) => {
        mockServer.on('connection', (socket) => {
            socket.send(
                JSON.stringify({
                    data: 'testing'
                })
            );
        });

        webSocketProtocol.message$().subscribe((message) => {
            expect(message).toEqual({
                data: 'testing'
            });
            done();
        });

        webSocketProtocol.connect();
    });

    it('should tigger close event', (done) => {
        webSocketProtocol.open$().subscribe(() => {
            mockServer.close();
        });

        webSocketProtocol.close$().subscribe(() => {
            done();
        });

        webSocketProtocol.error$().subscribe(() => {
            expect(true).toBe(false, 'Should not trigger error event');
        });

        webSocketProtocol.connect();
    });

    afterEach(() => {
        mockServer.close();
    });
});
