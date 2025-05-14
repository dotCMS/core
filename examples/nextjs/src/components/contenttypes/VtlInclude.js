"use client";

import { useIsEditMode } from "@/hooks/isEditMode";
import DestinationListing from "../DestinationListing";


// Learn more about widgetCodeJSON here: https://dev.dotcms.com/docs/scripting-api#ResponseJSON
export default function VtlInclude({ componentType, widgetCodeJSON }) {
    const isEditMode = useIsEditMode();

    if (componentType === "DestinationListing") {
        return <DestinationListing {...widgetCodeJSON} />;
    }

    if(isEditMode) {
        return <div>
            <h1>No Component Type: {componentType} Found for VTL Include</h1>
            <p>Component Type: {componentType}</p>
            <p>Widget Code JSON: {JSON.stringify(widgetCodeJSON)}</p>
        </div>;
    }

    return null
}

