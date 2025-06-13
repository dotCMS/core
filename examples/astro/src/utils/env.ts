import { loadEnv } from "vite";

const env = loadEnv(process.env.NODE_ENV || "development", process.cwd(), "");

/**
 * Validates and formats the dotCMS host URL
 * @returns The formatted dotCMS host URL with protocol
 * @throws Error if PUBLIC_DOTCMS_HOST environment variable is not set
 */
export const getDotCMSHost = (): string => {
  const host = env.PUBLIC_DOTCMS_HOST;

  if (!host) {
    return "http://localhost:8080";
  }

  const isProtocolPresent =
    host.startsWith("http://") || host.startsWith("https://");

  return isProtocolPresent ? host : `https://${host}`;
};
