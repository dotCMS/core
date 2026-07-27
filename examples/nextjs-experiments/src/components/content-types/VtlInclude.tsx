"use client";

import { useIsEditMode } from "@/hooks/useIsEditMode";
import DestinationListing from "../DestinationListing";

import type { Destination } from "@/types/content";

interface VtlIncludeProps {
    componentType?: string;
    widgetCodeJSON?: {
        destinations?: Destination[];
    };
}

// Learn more about widgetCodeJSON here: https://dev.dotcms.com/docs/scripting-api#ResponseJSON
export default function VtlInclude({ componentType, widgetCodeJSON }: VtlIncludeProps) {
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
