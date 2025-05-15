"use client";

import { useIsEditMode } from "@/hooks/isEditMode";
import DestinationListing from "../DestinationListing";

// Learn more about widgetCodeJSON here: https://dev.dotcms.com/docs/scripting-api#ResponseJSON
export default function VtlInclude({ componentType, widgetCodeJSON }) {
    const isEditMode = useIsEditMode();

    if (componentType === "destinationListing") {
        return <DestinationListing {...widgetCodeJSON} />;
    }

    if (isEditMode) {
        return (
            <div className="bg-blue-100 p-4">
                <h4>
                    No Component Type: {componentType || "generic"} Found for
                    VTL Include
                </h4>
            </div>
        );
    }

    return null;
}
