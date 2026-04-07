import { Injectable } from '@angular/core';

import { Subject } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class HashbrownChatBridgeService {
    private readonly userMessageSubject = new Subject<string>();
    private readonly openChatSubject = new Subject<void>();

    readonly userMessage$ = this.userMessageSubject.asObservable();
    readonly openChat$ = this.openChatSubject.asObservable();

    sendUserMessage(message: string): void {
        const trimmedMessage = message.trim();

        if (!trimmedMessage) {
            return;
        }

        this.userMessageSubject.next(trimmedMessage);
    }

    requestOpenChat(): void {
        this.openChatSubject.next();
    }
}
