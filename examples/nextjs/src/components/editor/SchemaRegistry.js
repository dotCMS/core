"use client";

import { useEffect } from "react";
import { registerStyleSchemas } from "@dotcms/uve";
import { componentSchemas } from "@/config/component-schemas";

/**
 * SchemaRegistry Component
 *
 * Registers all component style configurations once when the app loads.
 * This runs on the client side only and should be included in the root layout.
 */
export default function SchemaRegistry() {
    useEffect(() => {
        registerStyleSchemas(componentSchemas);
    }, []); // Empty dependency array ensures this runs only once

    // This component doesn't render anything
    return null;
}
