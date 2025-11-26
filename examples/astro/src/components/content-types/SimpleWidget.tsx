import { useIsEditMode } from "@/hooks";

import type { DotCMSBasicContentlet } from "@dotcms/types";

const TRAVEL_BOT_KEY = "908b8a434ad7e539632b8db57f2967c0";

interface SimpleWidgetProps extends DotCMSBasicContentlet {
  widgetTitle: string;
  identifier: string;
  code: string;
}
export default function SimpleWidget({
  widgetTitle,
  identifier
}: SimpleWidgetProps) {
  const isEditMode = useIsEditMode();
  if (TRAVEL_BOT_KEY == identifier) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-8 text-center max-w-lg mx-auto my-6">
        <h2 className="text-2xl font-bold text-gray-800 mb-4">
          WELCOME TO TRAVELBOT
        </h2>

        <p className="text-gray-600 mb-4">
          TravelBot is built with <span className="font-medium">dotAI</span>,
          the dotCMS suite of AI features.
        </p>

        <p className="text-gray-600">
          Please configure the dotAI App to enable dotAI and TravelBot.
        </p>
      </div>
    );
  }

  if (isEditMode) {
    return (
      <div
        className="p-4 mb-4 text-sm text-blue-800 rounded-lg bg-blue-50 dark:bg-gray-800 dark:text-blue-400"
        role="alert"
      >
        <h4>Simple Widget: {widgetTitle}</h4>
      </div>
    );
  }

  return null;
}
