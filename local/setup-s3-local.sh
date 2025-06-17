#!/bin/bash

set -e

ORG_ID="20587698-1b67-4cbb-8a08-d7e9fe56a77d"
FILIAL_ID="e741e0b4-02f9-4e6e-b3c3-4318d36477b3"
IMAGE_PATH="/resources/images/primeira_igreja_logo_store_invertido_cor.png"
BUCKET="storehouse-images"
ENDPOINT="http://localstack:4566"

sleep 3

echo "🔍 Testando conexão com LocalStack:"
curl -s http://localstack:4566/_localstack/health || echo "❌ LocalStack indisponível via curl"

echo "..."

echo "⏳ Aguardando LocalStack ficar disponível..."
until aws --endpoint-url=$ENDPOINT s3 ls > /dev/null 2>&1; do
  sleep 2
done

echo "✅ LocalStack disponível. Criando bucket (caso não exista)..."
aws --endpoint-url=$ENDPOINT s3 mb s3://$BUCKET || true

echo "⬆️ Enviando imagem..."
aws --endpoint-url=$ENDPOINT s3 cp "$IMAGE_PATH" "s3://$BUCKET/$ORG_ID/$FILIAL_ID/logo.png"

echo "✅ Upload finalizado!"
echo "URL esperada:"
echo "https://primeiraigrejastoretest.loca.lt/$BUCKET/$ORG_ID/$FILIAL_ID/logo.png"