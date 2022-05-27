<!-- @format -->

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8" />
        <meta name="robots" content="noindex" />
        <meta name="referrer" content="origin" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>DotCMS GraphQL Playground</title>
        <link rel="stylesheet" type="text/css" href="swagger-ui.css">
    </head>
    <body>
        <div id="swagger-ui"></div>

        <script src="swagger-ui-bundle.js"></script>
        <script src="swagger-ui-standalone-preset.js"></script>

        <script>
            window.onload = function() {
                const ui = SwaggerUIBundle({
                    url: "../../../../api/openapi.json",
                    // deepLinking: true,
                    dom_id: '#swagger-ui',
                    presets: [
                        SwaggerUIBundle.presets.apis,
                        SwaggerUIStandalonePreset
                    ]
                })

                window.ui = ui
            }
        </script>

    </body>
</html>
