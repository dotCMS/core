import { JSONSchemaToZod } from "@dmitryrechkin/json-schema-to-zod";
import { z, ZodObject } from "zod";

export function convertJsonToZodSchema(json: any) {

    const zodSchema = JSONSchemaToZod.convert(json) as ZodObject<any>;

    const newSchema = zodSchema.extend({
        category: z.enum(["Option 1", "Option 2", "Option 3"])
      })

    

    return newSchema;
}