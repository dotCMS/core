import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

interface FieldStructure {
    dataType: string;
    format: string;
    key: string;
    type?: string;
    variable?: string;
}

@Injectable()
export class OpenAiService {
    // Define la URL de la API, que se usará para las solicitudes
    private apiUrl = 'http://localhost:3000/ai/content-generator';
    private apiKey = 'YOUR_API_KEY'; //Thanks Jal!
    private http = inject(HttpClient);

    // Método para enviar un mensaje a la API mediante una solicitud POST
    sendMessage(body: {
        topic: string;
        tone: string;
        language: string;
        formStructure: Record<string, FieldStructure>;
    }) {
        return this.http
            .post<{ result: Record<string, string> }>(this.apiUrl, body)
            .pipe(map((response) => response.result));
    }
}
