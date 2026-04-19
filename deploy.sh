#!/bin/bash
# CodeCrack Deploy Script
# Works on any cloud: DigitalOcean, AWS, Azure

echo "🚀 CodeCrack Deploying..."

# Pull latest code
git pull origin main

# Stop existing containers
docker-compose down

# Build and start
docker-compose up -d --build

# Wait for health
echo "⏳ Waiting for services..."
sleep 15

# Check status
docker-compose ps

echo "✅ CodeCrack deployed! http://$(curl -s ifconfig.me):8080"
