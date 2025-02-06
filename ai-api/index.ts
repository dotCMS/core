import { serve } from "@hono/node-server";
import { Hono } from "hono";
import { cors } from "hono/cors";
import { prettyJSON } from "hono/pretty-json";
import { RunnableSequence } from "@langchain/core/runnables";
import { StringOutputParser } from "@langchain/core/output_parsers";
import { ChatPromptTemplate } from "@langchain/core/prompts";
import { JSONSchemaToZod } from '@dmitryrechkin/json-schema-to-zod';
import "dotenv/config";

import { ChatOpenAI } from "@langchain/openai";

const model = new ChatOpenAI({ model: "gpt-4o-mini", });

const app = new Hono();

app.use("*", cors());
app.use("*", prettyJSON());


app.get('/', async (c) => {
    const response = await model.invoke('Hello')

    return c.json(response.content)
  })

  app.post('/ai/content-generator', async (c) => {

    const { topic, tone, language, schema } = await c.req.json();

    const promptTemplate = ChatPromptTemplate.fromMessages([
        ["system", "You are a content genetor. You will be given a topic, a tone, and a language. You will need to generate a contentlet based on the topic, tone, and language." ],
        ["user", "Generate a contentlet based on the following topic: {topic}, tone: {tone}, and language: {language}"],
      ]);

      

      const zodSchema = JSONSchemaToZod.convert(schema);



      const structuredLlm = model.withStructuredOutput(zodSchema);

    const chain = RunnableSequence.from([
        promptTemplate,
        structuredLlm,
      ]);

      const result = await chain.invoke({topic, tone, language});

    return c.json({result})
  })

  app.post('/ai/refine', async (c) => {
    const { text, tone, language } = await c.req.json();


    const promptTemplate = ChatPromptTemplate.fromMessages([
        ["system", "You are a text refiner. You will be given a text, a tone, and a language. You will need to refine the text to the tone and language."],
        ["user", "Refine this text <text>{text}</text> in this tone: <tone>{tone}</tone> and language: <language>{language}</language>"],
      ]);

    const chain = RunnableSequence.from([
        promptTemplate,
        model,
        new StringOutputParser(),
      ]);

      const result = await chain.invoke({
        text,
        tone,
        language
      });

    return c.json({result})
  })
  

  serve({
      fetch: app.fetch,
      port: 3000,
  });