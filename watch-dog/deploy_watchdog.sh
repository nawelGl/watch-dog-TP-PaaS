#!/usr/bin/env bash
set -euo pipefail

# ========= À ADAPTER =========
VM_USER="vm-watch-dog"
VM_HOST="172.31.249.254"
REMOTE_DIR="/home/${VM_USER}/watch-dog"
SSH_KEY="$HOME/.ssh/id_rsa"          # adapte si besoin (id_ed25519 etc.)
IMAGE_NAME="watchdog:latest"
JAR_PATH="target/watch-dog-0.0.1-SNAPSHOT.jar"
# =============================

ssh_cmd() { ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "${VM_USER}@${VM_HOST}" "$@"; }
scp_cmd() { scp -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$@"; }

echo "========================================"
echo "🚀 BUILD JAR (Maven)"
echo "========================================"
mvn clean package -DskipTests

test -f "$JAR_PATH" || { echo "❌ JAR absent: $JAR_PATH"; exit 1; }
echo "✅ JAR généré : $JAR_PATH"

echo "========================================"
echo "🐳 BUILD IMAGE (sur ton Mac)"
echo "========================================"
docker build --platform=linux/amd64 -t "$IMAGE_NAME" .
#docker build --platform=linux/amd64 -t watchdog:latest .

echo "========================================"
echo "📦 EXPORT IMAGE"
echo "========================================"
rm -f watchdog-image.tar.gz
docker save "$IMAGE_NAME" | gzip > watchdog-image.tar.gz

echo "========================================"
echo "📁 PRÉPARATION VM"
echo "========================================"
ssh_cmd "mkdir -p '${REMOTE_DIR}/config'"

echo "========================================"
echo "📤 UPLOAD (compose + config + image)"
echo "========================================"
scp_cmd docker-compose.yml "${VM_USER}@${VM_HOST}:${REMOTE_DIR}/"
scp_cmd src/main/resources/application.yml "${VM_USER}@${VM_HOST}:${REMOTE_DIR}/config/"
scp_cmd watchdog-image.tar.gz "${VM_USER}@${VM_HOST}:/home/${VM_USER}/"

echo "========================================"
echo "📥 LOAD IMAGE + RUN (sur la VM) : zéro pull"
echo "========================================"
ssh_cmd "gunzip -c /home/${VM_USER}/watchdog-image.tar.gz | docker load"

# (optionnel) stop ancien conteneur si existe, puis up
ssh_cmd "cd '${REMOTE_DIR}' && docker compose up -d"

echo "========================================"
echo "📊 STATUS + LOGS"
echo "========================================"
ssh_cmd "docker ps"
ssh_cmd "docker logs --tail=120 watchdog || true"

echo "✅ Deploy terminé (image build sur ton Mac, VM sans pull, SSH via /home/vm-watch-dog/.ssh)"