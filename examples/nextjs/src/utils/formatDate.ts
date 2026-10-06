const dateFormatOptions: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "long",
  day: "numeric",
};

/** Formats a dotCMS `modDate`-style value as e.g. "January 1, 2025". */
export function formatDate(date?: string): string {
  return new Date(date ?? "").toLocaleDateString("en-US", dateFormatOptions);
}
