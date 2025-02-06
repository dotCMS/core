import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

const getAutoFillPrompt = (
    contentType: string,
    structure: string,
    description: string,
    language: string
) => `
I have a form that represents the structure of a content type called "${contentType}" with the following structure:

${structure}

Each object in the structure follows this format:
\`\`\`
[
  {
    "variable": "The field name",
    "format": "Expected format or constraints",
    "dataType": "The expected data type (e.g., string, number, boolean, HTML, etc.)"
  }
]
\`\`\`

Please generate values for each field while ensuring the following:

1. Each field value strictly matches the expected **data type** and **format** defined in \`${structure}\`.
2. The values are contextually relevant based on this description, which provides details and a theme for generating appropriate content:
   - **Description:** ${description}

For the **Story-Block** field, follow these additional rules:

1. The Story-Block must be a **valid HTML string**.
2. It must contain **at least 4 paragraphs**.
3. It must include **at least 2 quotes**.
4. It must contain **at least 2 images** (use a placeholder image; **DO NOT generate images**, but reference the placeholder URL: \`https://placehold.co/\`).

### **Response Format:**
The response **must** be a valid JSON object, structured as follows:

- Each key in the JSON should use the **"variable"** field from \`${structure}\`.
- The corresponding value should be AI-generated, adhering to the specified **format** and **data type**.
- You must generate the content in the language specified in the prompt: ${language}

#### **Example Output Format:**
\`\`\`json
{
  "[VARIABLE_NAME_FROM_STRUCTURE]": "Generated value",
  "[ANOTHER_VARIABLE_NAME]": "Generated value"
}
\`\`\`

### **Additional Constraints:**
- The response **must contain only the JSON object**, with no additional text or explanation.
- Ensure proper JSON formatting so it is **fully valid and parseable**.
- Ensue the JSON never ends with a comma.

**BAD Example***
\`\`\`
{
    "variable": "The field value",
}
\`\`\`

**GOOD Example**
\`\`\`
{
    "variable": "The field value"
}
\`\`\`
`;

@Injectable()
export class OpenAiService {
    // Define la URL de la API, que se usará para las solicitudes
    private apiUrl = 'https://api.openai.com/v1/chat/completions';
    private apiKey = 'YOUR_OPENAI_API_KEY'; //Thanks Jal!
    private http = inject(HttpClient);

    // Método para enviar un mensaje a la API mediante una solicitud POST
    sendMessage({
        contentType,
        structure,
        description,
        language
    }: {
        contentType: string;
        structure: string;
        description: string;
        language: string;
    }) {
        // Configura los encabezados de la solicitud, incluyendo la autorización con Bearer token
        const headers = new HttpHeaders({
            'Content-Type': 'application/json', // Indica que el contenido es JSON
            Authorization: `Bearer ${this.apiKey}` // Autenticación usando la clave API
        });

        // Cuerpo de la solicitud con el modelo y el mensaje del usuario
        const body = {
            model: 'gpt-3.5-turbo', // Especifica el modelo de lenguaje que se está usando
            messages: [
                {
                    role: 'user',
                    content: getAutoFillPrompt(contentType, structure, description, language)
                }
            ] // Mensaje del usuario que se envía a la API
        };

        // Realiza la solicitud POST a la API y retorna un Observable para manejar la respuesta
        return this.http
            .post(this.apiUrl, body, { headers })
            .pipe(
                map(
                    (response: { choices: { message: { content: string } }[] }) =>
                        response.choices[0].message.content
                )
            );
    }
}
