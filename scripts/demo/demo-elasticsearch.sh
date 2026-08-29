#!/usr/bin/env bash

set -e

ELASTIC_URL="http://localhost:9200"
INDEX="leasedemo-leases"

echo "========================================"
echo " LeaseDemo — Elasticsearch Demo"
echo " Global EU Lease Search"
echo "========================================"
echo

echo "[1/4] Elasticsearch health:"

curl -s \
    "$ELASTIC_URL/_cluster/health?pretty"

echo
echo

echo "[2/4] Search: Fiat Tipo"

curl -s \
    -X GET \
    "$ELASTIC_URL/$INDEX/_search?pretty" \
    -H 'Content-Type: application/json' \
    -d '
{
  "query": {
    "match": {
      "vehicle": "Fiat Tipo"
    }
  }
}
'

echo
echo

echo "[3/4] Search with typo: Fiat Tpio"

curl -s \
    -X GET \
    "$ELASTIC_URL/$INDEX/_search?pretty" \
    -H 'Content-Type: application/json' \
    -d '
{
  "query": {
    "match": {
      "vehicle": {
        "query": "Fiat Tpio",
        "fuzziness": "AUTO"
      }
    }
  }
}
'

echo
echo

echo "[4/4] Expected:"
echo
echo "PL:"
echo "  Jan Kowalski | Fiat Tipo"
echo
echo "DE:"
echo "  Hans Mueller | Fiat Tipo"
echo
echo "Both returned from ONE Elasticsearch search index."
