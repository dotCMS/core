import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

const API_KEY = '';

interface Message {
    role: string;
    content: string;
}

interface Choice {
    message: Message;
    finish_reason: string;
}

interface OpenAIResponse {
    id: string;
    object: string;
    created: number;
    model: string;
    usage: {
        prompt_tokens: number;
        completion_tokens: number;
        total_tokens: number;
    };
    choices: Choice[];
}

@Injectable({
    providedIn: 'root'
})
export class AiContentService {
    constructor(private http: HttpClient) {}

    getIAContent(prompt: string): Observable<string> {
        const body = JSON.stringify({
            model: 'gpt-3.5-turbo',
            messages: [
                {
                    role: 'system',
                    content: 'Transforms answer on user prompts into detailed HTML responses.'
                },
                {
                    role: 'user',
                    content: prompt
                }
            ]
        });

        const headers = new HttpHeaders({
            Authorization: `Bearer ${API_KEY}`,
            'Content-Type': 'application/json'
        });

        return this.http
            .post<OpenAIResponse>('https://api.openai.com/v1/chat/completions', body, { headers })
            .pipe(
                catchError(() => {
                    return throwError('Error fetching AI content');
                }),
                map((response) => {
                    const messageResponse = response.choices?.[0]?.message?.content ?? '';

                    return messageResponse;
                })
            );
    }
}
