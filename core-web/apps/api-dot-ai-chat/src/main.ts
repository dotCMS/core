import { HashbrownOpenAI } from '@hashbrownai/openai';
import express from 'express';

const apiKey = process.env['OPENAI_API_KEY'];

if (!apiKey) {
    throw new Error('OPENAI_API_KEY is not set');
}

const app = express();

app.use(express.json());
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    res.header('Access-Control-Allow-Methods', 'GET,POST,PUT,PATCH,DELETE,OPTIONS');

    if (req.method === 'OPTIONS') {
        res.sendStatus(204);
        return;
    }

    next();
});

app.post('/dotaichat/chat', async (req, res) => {
    const stream = HashbrownOpenAI.stream.text({
        apiKey,
        request: req.body
    });

    res.header('Content-Type', 'application/octet-stream');

    for await (const chunk of stream) {
        res.write(chunk);
    }

    res.end();
});

const port = process.env.PORT || 3333;
const server = app.listen(port, () => {
    // eslint-disable-next-line no-console
    console.log(`Listening at http://localhost:${port}/dotaichat`);
});
server.on('error', console.error);
