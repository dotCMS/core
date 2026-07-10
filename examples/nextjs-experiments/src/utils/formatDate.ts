const dateFormatOptions: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "long",
  day: "numeric",
};

/**
 * Formats a dotCMS `modDate`-style value as e.g. "January 1, 2025".
 * Returns an empty string when the date is missing or unparseable, so the UI
 * never renders the literal "Invalid Date".
 */
export function formatDate(date?: string): string {
  if (!date) {
    return "";
  }

  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) {
    return "";
  }

  return parsed.toLocaleDateString("en-US", dateFormatOptions);
}
