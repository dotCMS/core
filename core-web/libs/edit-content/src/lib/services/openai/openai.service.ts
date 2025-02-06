import { HttpClient, HttpHeaders } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";

import { map } from "rxjs/operators";

const getAutoFillPrompt = (contentType: string, structure: string, description: string) => `
I have a form that represents the structure of a content type called ${contentType}, with the following structure:

${structure}

Please fill in the fields while ensuring that:

Each field value matches the expected data type defined in $STRUCTURE.
The values are contextually relevant based on the following description, which provides more details and the theme for generating appropriate content:
${description}

For the Story-Block field, the response must be a valid HTML string, and the values must be contextually relevant based on the following rules: 

1. The Story-Block must be a valid HTML string.
2. The Story-Block must to have a minimum of 4 paragraphs.
3. The Story-Block must to have a minimum of 2 quotes.
4. The Story-Block must to have a minimum of 2 images (Using a placeholder image, DO NOT GENERATE IMAGES, use placeholder based on this API URL https://placehold.co/).

The response must be a valid JSON object that strictly follows the same structure as ${structure}, but with the fields populated based on the generated values.

The output must contain only the JSON object and no additional text.
Ensure proper formatting so the JSON is parseable and valid.
`

@Injectable()
export class OpenAiService {
    // Define la URL de la API, que se usará para las solicitudes
    private apiUrl = 'https://api.openai.com/v1/chat/completions';
    private apiKey = 'YOUR_API_KEY'; //Thanks Jal!
    private http = inject(HttpClient);


    // Método para enviar un mensaje a la API mediante una solicitud POST
    sendMessage({ contentType, structure, description }: { contentType: string, structure: string, description: string }) {
        // Configura los encabezados de la solicitud, incluyendo la autorización con Bearer token
        const headers = new HttpHeaders({
            'Content-Type': 'application/json', // Indica que el contenido es JSON
            'Authorization': `Bearer ${this.apiKey}` // Autenticación usando la clave API
        });

        // Cuerpo de la solicitud con el modelo y el mensaje del usuario
        const body = {
            model: 'gpt-3.5-turbo', // Especifica el modelo de lenguaje que se está usando
            messages: [{ role: 'user', content: getAutoFillPrompt(contentType, structure, description) }] // Mensaje del usuario que se envía a la API
        };

        // Realiza la solicitud POST a la API y retorna un Observable para manejar la respuesta
        return this.http.post(this.apiUrl, body, { headers }).pipe(
            map((response: { choices: { message: { content: string } }[] }) => response.choices[0].message.content)
        );
    }
}
