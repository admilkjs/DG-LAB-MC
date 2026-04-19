param(
    [string]$Date = (Get-Date -Format 'yyyyMMdd')
)

$salt = 'admilk-dglab-lock-v1'
$bytes = [System.Text.Encoding]::UTF8.GetBytes("$salt`:$Date")
$sha256 = [System.Security.Cryptography.SHA256]::Create()
try {
    $hash = $sha256.ComputeHash($bytes)
} finally {
    $sha256.Dispose()
}

$hex = -join ($hash | ForEach-Object { $_.ToString('x2') })
$password = $hex.Substring(0, 10).ToUpperInvariant()

Write-Output "Date: $Date"
Write-Output "Password: $password"
