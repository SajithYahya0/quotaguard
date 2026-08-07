param([string]$Key)

$ok = 0; $limited = 0
1..40 | ForEach-Object {
    $port = if ($_ % 2 -eq 0) { 8080 } else { 8081 }
    try {
        Invoke-WebRequest -Uri "http://localhost:$port/api/data/quote" `
            -Headers @{ "X-API-Key" = $Key } -UseBasicParsing | Out-Null
        $ok++
    } catch {
        $limited++
    }
}

Write-Host "capacity 10  ->  200 OK: $ok   429: $limited"