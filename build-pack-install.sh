#!/bin/bash

# Script para hacer build, pack e instalar paquetes SDK en la librería de ejemplo

set -e

# Función para mostrar ayuda
show_help() {
    echo "📚 Uso: $0 --project=<nombre-proyecto>"
    echo ""
    echo "Parámetros:"
    echo "  --project=<nombre>    Nombre del proyecto SDK a hacer build (ej: sdk-analytics, sdk-client, sdk-experiments)"
    echo "  --help               Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0 --project=sdk-analytics"
    echo "  $0 --project=sdk-client"
    echo "  $0 --project=sdk-experiments"
    exit 0
}

# Variables por defecto
PROJECT=""

# Parsear argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        --project=*)
            PROJECT="${1#*=}"
            shift
            ;;
        --help|-h)
            show_help
            ;;
        *)
            echo "❌ Error: Parámetro desconocido '$1'"
            echo "Usa --help para ver las opciones disponibles"
            exit 1
            ;;
    esac
done

# Validar que se proporcionó el proyecto
if [ -z "$PROJECT" ]; then
    echo "❌ Error: Debe especificar el proyecto con --project=<nombre>"
    echo "Usa --help para ver las opciones disponibles"
    exit 1
fi

echo "🚀 Iniciando proceso de build, pack e instalación para: $PROJECT"

# Variables - Rutas absolutas desde el directorio core
CORE_DIR="/Users/arcadioquintero/Work/core"
PROJECT_SOURCE_PATH="$CORE_DIR/core-web/libs/sdk/${PROJECT#sdk-}"
PROJECT_DIST_PATH="$CORE_DIR/core-web/dist/libs/sdk/${PROJECT#sdk-}"
EXAMPLE_PATH="$CORE_DIR/examples/nextjs"
CURRENT_DIR=$(pwd)

# Función para mostrar mensajes
log() {
    echo "📋 $1"
}

# Verificar que el proyecto existe
if [ ! -d "$PROJECT_SOURCE_PATH" ]; then
    log "❌ Error: El proyecto '$PROJECT' no existe en $PROJECT_SOURCE_PATH"
    log "Proyectos disponibles:"
    ls -1 "$CORE_DIR/core-web/libs/sdk/" | sed 's/^/  - sdk-/'
    exit 1
fi

# Paso 1: Build del paquete
log "Building paquete $PROJECT..."
cd "$CORE_DIR/core-web"
npx nx build "$PROJECT" || {
    log "❌ Error: No se pudo hacer build del paquete $PROJECT"
    exit 1
}

# Verificar que el build se completó
if [ ! -d "$PROJECT_DIST_PATH" ]; then
    log "❌ Error: El directorio de distribución no fue creado en $PROJECT_DIST_PATH"
    exit 1
fi

cd "$PROJECT_DIST_PATH"

# Paso 2: Pack del paquete
log "Empaquetando..."
PACK_FILE=$(npm pack --silent)
log "Paquete creado: $PACK_FILE"

# Paso 3: Instalar en la librería de ejemplo
cd "$EXAMPLE_PATH"
log "Instalando paquete en ejemplo NextJS..."

# Obtener el nombre del paquete desde package.json del proyecto
PACKAGE_NAME=$(node -p "require('$PROJECT_DIST_PATH/package.json').name")

# Remover instalación anterior si existe
npm uninstall "$PACKAGE_NAME" 2>/dev/null || true

# Instalar el nuevo paquete
npm install "$PROJECT_DIST_PATH/$PACK_FILE"

log "✅ Proceso completado exitosamente!"
log "📦 Paquete $PACKAGE_NAME instalado en $EXAMPLE_PATH"
