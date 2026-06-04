@echo off
echo Starting ngrok tunnel to localhost:8080...
echo.
echo After ngrok starts, update your GitHub webhook URL to:
echo https://YOUR-NGROK-URL/api/webhook/github
echo.
ngrok http 8080
