import { useEffect, useState } from "react";

import { getUVEState } from "@dotcms/uve";
import { UVE_MODE } from "@dotcms/types";

/**
 * Check if the user is in edit mode
 * @returns {boolean}
 */
export function useIsEditMode(): boolean {
  const [isEditMode, setIsEditMode] = useState(false);

  useEffect(() => {
    setIsEditMode(getUVEState()?.mode === UVE_MODE.EDIT);
  }, []);

  return isEditMode;
}
