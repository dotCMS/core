/**
 * Centralized component style schema configurations
 *
 * Define all component style schemas here. These will be registered
 * once when the application loads on the client side.
 */

export const componentSchemas = [
    {
        contentType: "Banner",
        configuration: {
            title: {
                color: {
                    type: "single select",
                    options: ["#000000", "#FFFFFF", "#000000", "#FFFFFF"]
                },
            },
        }
    },
    {
        contentType: "Activity",
        configuration: {
            title: {
                color: {
                    type: "Multiple Select",
                    options: ["#000000", "#FFFFFF", "#000000", "#FFFFFF"]
                },
            },
        }
    }
]
