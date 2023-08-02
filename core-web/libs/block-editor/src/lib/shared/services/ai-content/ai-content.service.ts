import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

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

    async fetchAIContent(prompt: string): Promise<string> {
        const API_KEY = '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';

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

        const response: OpenAIResponse = await this.http
            .post<OpenAIResponse>('https://api.openai.com/v1/chat/completions', body, { headers })
            .toPromise();

        let messageResponse = '';
        if (response.choices && response.choices.length > 0) {
            messageResponse = response.choices[0].message.content;
        }

        return messageResponse;
    }
}
