$env:MINIO_ROOT_USER = "minioadmin"
$env:MINIO_ROOT_PASSWORD = "minioadmin"

if (-not (Test-Path "D:\minio-data")) {
    New-Item -ItemType Directory -Force -Path "D:\minio-data" | Out-Null
}

Write-Host "Starting MinIO..."
Write-Host "  API:      http://localhost:9000"
Write-Host "  Console:  http://localhost:9001"
Write-Host "  User:     minioadmin / minioadmin"
Write-Host ""

& minio server D:\minio-data --console-address ":9001"