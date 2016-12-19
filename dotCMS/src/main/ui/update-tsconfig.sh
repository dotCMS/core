#!/usr/bin/env bash
echo "{" > ./tsconfig.json
echo "  \"compilerOptions\": {" >> ./tsconfig.json
echo "    \"target\": \"ES5\"," >> ./tsconfig.json
echo "    \"module\": \"system\"," >> ./tsconfig.json
echo "    \"moduleResolution\": \"node\"," >> ./tsconfig.json
echo "    \"sourceMap\": true," >> ./tsconfig.json
echo "    \"emitDecoratorMetadata\": true," >> ./tsconfig.json
echo "    \"experimentalDecorators\": true," >> ./tsconfig.json
echo "    \"removeComments\": false," >> ./tsconfig.json
echo "    \"noImplicitAny\": false," >> ./tsconfig.json
echo "    \"suppressImplicitAnyIndexErrors\":true," >> ./tsconfig.json
echo "    \"outDir\": \"./build/\"" >> ./tsconfig.json
echo "  }," >> ./tsconfig.json
echo "  \"files\":[" >> ./tsconfig.json


find ./src -name '*.ts' | while read line; do
    echo "    \"$line\"," >> ./tsconfig.json
done

#find ./typings -name '*.ts' | while read line; do
#    echo "    \"$line\"," >> ./tsconfig.json
#done

#echo "    \"./typings/custom/normalizr.d.ts\"," >> ./tsconfig.json
#echo "    \"./node_modules/immutable/dist/immutable.d.ts\"," >> ./tsconfig.json
echo "    \"./typings/main.d.ts\"" >> ./tsconfig.json
echo "  ]" >> ./tsconfig.json
echo "}" >> ./tsconfig.json