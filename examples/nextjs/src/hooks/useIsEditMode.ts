"use client";

import { useEffect, useState } from "react";

import { UVE_MODE } from "@dotcms/types";
import { getUVEState } from "@dotcms/uve";

/**
 * Returns `true` when the page is being rendered inside the dotCMS Universal
 * Visual Editor in edit mode. Runs on the client only, so it starts `false`
 * and updates after mount.
 */
export function useIsEditMode(): boolean {
  const [isEditMode, setIsEditMode] = useState(false);

  useEffect(() => {
    // UVE state only exists in the browser after hydration, so it must be read
    // in an effect (not during render) to avoid a server/client mismatch.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsEditMode(getUVEState()?.mode === UVE_MODE.EDIT);
  }, []);

  return isEditMode;
}
