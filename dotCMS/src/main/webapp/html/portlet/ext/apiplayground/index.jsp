<!-- @format -->

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8" />
        <meta name="robots" content="noindex" />
        <meta name="referrer" content="origin" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>DotCMS API Playground</title>
        <link rel="stylesheet" type="text/css" href="swagger-ui.css?v=20240819">
    </head>
    <body>
        <div id="swagger-ui"></div>

        <script src="swagger-ui-bundle.js?v=20240819"></script>
        <script src="swagger-ui-standalone-preset.js?v=20240819"></script>

        <script>
            const HideSchemes = function() {
                return {
                    components: {
                        schemes: function() {
                            return null
                        }
                    }
                }
            }

            window.onload = function() {
                const ui = SwaggerUIBundle({
                    url: "../../../../api/openapi.json",
                    dom_id: '#swagger-ui',
                    presets: [
                        SwaggerUIBundle.presets.apis,
                        SwaggerUIStandalonePreset
                    ],
                    plugins: [HideSchemes],
                    layout: "BaseLayout",
                    operationsSorter: 'alpha',
                })

                window.ui = ui
            }
        </script>

    </body>
</html>
