#!/bin/bash

set -e

mkdir -p /custom/dotsecure/license
touch /custom/dotsecure/license/license.dat
chmod 777 /custom/dotsecure/license/license.dat

# Today plus 15 days
expireDate=$(date -d "+15 days" '+%Y-%m-%d')

TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGljYTdmZTg2Ni0wMmU0LTQ5YjEtYjM4ZC0zODkwYjA0ZjcxMjkiLCJ4bW9kIjoxNTYyOTQyMjUzMDAwLCJuYmYiOjE1NjI5NDIyNTMsImlzcyI6IjNjYjk5OWIzLWE4YmItNDhhNC1iMDUzLTA3YjBmYThiZTJmMCIsImxhYmVsIjoiTGljZW5zZSBSZXF1ZXN0cyIsImV4cCI6MTY1NzU5ODQwMCwiaWF0IjoxNTYyOTQyMjUzLCJqdGkiOiIxN2U1ZWNkMS0yOGRmLTQ0M2MtOTdiNy0zY2Y0NGY0ZTZiNzQifQ.ENgL9LWnvEjinDuMcp-3wtiWmb1l7a4T2MwE2bCeynA"

curl -s -H "Content-Type: application/json" -H "Authorization:Bearer $TOKEN" -XPUT https://dotcms.com/api/ext/license -d '
  {
      "level": 500,
      "licenseType": "trial",
      "clientName": "docker Integration Tests",
      "clientEmail": "dockerTests@dotcms.com",
      "expireDate": "$expireDate"
  }
' >> /custom/dotsecure/license/license.dat
