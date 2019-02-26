import { WebSocketProtocol } from './websockets-protocol';
import { StringUtils } from '../string-utils.service';
import { LoggerService } from '../logger.service';
import { Server } from 'mock-socket';

describe('LongPollingProtocol', () => {
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
        mockServer.on('connection', socket => {
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
        webSocketProtocol.close$().subscribe(() => {
            done();
        });

        webSocketProtocol.connect();
        mockServer.close();
    });

    it('should tigger error event', (done) => {
        mockServer.close();

        webSocketProtocol.error$().subscribe(() => {
            done();
        });

        webSocketProtocol.connect();
    });

    afterEach(() => {
        mockServer.close();
    });
});
