export interface Template {
  friendlyName: string;
  identifier: string;
  image: string;
  theme: string;
  title: string;
  layout?: Record<string, unknown>;
}
