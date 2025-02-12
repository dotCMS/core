import { serve } from "@hono/node-server";
import { ChatPromptTemplate } from "@langchain/core/prompts";
import { RunnableSequence } from "@langchain/core/runnables";
import { Hono } from "hono";
import { cors } from "hono/cors";
import { prettyJSON } from "hono/pretty-json";
import { z } from "zod";

import "dotenv/config";

import { ChatOpenAI } from "@langchain/openai";

const model = new ChatOpenAI({ model: "gpt-4-0125-preview" });

const app = new Hono();

app.use("*", cors());
app.use("*", prettyJSON());

app.get("/", async (c) => {
  const response = await model.invoke("Hello");

  return c.json(response.content);
});

app.post("/ai/content-generator", async (c) => {
  const { topic, tone, language, formStructure } = await c.req.json();

  interface FieldStructure {
    dataType: string;
    format: string;
    key: string;
    type?: string;
    variable?: string;
  }

  // Convertir el formStructure a un schema de Zod
  const zodSchema = z.object(
    Object.entries(
      formStructure as Record<string, FieldStructure>
    ).reduce<z.ZodRawShape>((acc, [key, field]) => {
      if (field.dataType === "string[]") {
        acc[key] = z.array(z.string());
      } else {
        acc[key] = z.string();
      }
      return acc;
    }, {})
  );

  const promptTemplate = ChatPromptTemplate.fromMessages([
    [
      "system",
      "You are a content generator. Generate content that matches exactly the structure provided. Return only the requested fields with valid values.",
    ],
    [
      "user",
      "Generate content for:\nTopic: {topic}\nTone: {tone}\nLanguage: {language}\n\nUse exactly this structure: {structure}",
    ],
  ]);

  const structuredLlm = model.withStructuredOutput(zodSchema);
  const chain = RunnableSequence.from([promptTemplate, structuredLlm]);

  const result = await chain.invoke({
    topic,
    tone,
    language,
    structure: JSON.stringify(formStructure, null, 2),
  });

  return c.json({ result });
});

app.post("/ai/refine", async (c) => {
  const { text, tone, language, system } = await c.req.json();

  const promptTemplate = ChatPromptTemplate.fromMessages([
    ["system", system],
    [
      "user",
      `Please improve the following text:

Original text: "${text}"
Desired tone: ${tone}
Language: ${language}

Please provide an improved version that maintains the core meaning.`,
    ],
  ]);

  const structuredLlm = model.withStructuredOutput(
    z.object({
      text: z.string().describe("The improved version of the text"),
      explanation: z
        .string()
        .describe("Brief explanation of what was improved"),
    })
  );

  const chain = RunnableSequence.from([promptTemplate, structuredLlm]);

  const result = await chain.invoke({
    text,
    tone,
    language,
  });
  console.log(result);
  return c.json(result);
});

serve({
  fetch: app.fetch,
  port: 3000,
});
