param(
    [Parameter(Mandatory = $true)]
    [string]$PlayerName,

    [string]$Trigger = 'player_death',

    [string]$Date = (Get-Date -Format 'yyyyMMdd'),

    [switch]$SignalOnly
)

$supportedTriggers = @(
    'player_hurt',
    'player_heal',
    'player_death',
    'player_killed_by_other',
    'low_health_threshold',
    'low_food_threshold',
    'jump',
    'fall_damage',
    'sprint_start',
    'sneak_start',
    'shield_block',
    'attack_damage_all',
    'attack_damage_player',
    'attack_damage_non_player',
    'attack_critical_all',
    'attack_critical_player',
    'attack_critical_non_player',
    'attack_kill_all',
    'attack_kill_player',
    'attack_kill_non_player',
    'bow_release',
    'totem_trigger',
    'armor_low'
)

$salt = 'admilk-dglab-signal-v1'
$target = $PlayerName.Trim().ToLowerInvariant()
$eventId = $Trigger.Trim().ToLowerInvariant()

if ([string]::IsNullOrWhiteSpace($target)) {
    throw 'PlayerName is required'
}

if ($eventId -eq 'death') {
    $eventId = 'player_death'
}

if ($supportedTriggers -notcontains $eventId) {
    throw ('Unsupported trigger: ' + $eventId + '. Supported: ' + ($supportedTriggers -join ', '))
}

$keySource = [System.Text.Encoding]::UTF8.GetBytes("$salt`:$Date")
$sha256 = [System.Security.Cryptography.SHA256]::Create()
try {
    $hash = $sha256.ComputeHash($keySource)
} finally {
    $sha256.Dispose()
}

$key = New-Object byte[] 16
[Array]::Copy($hash, 0, $key, 0, 16)

$payload = "v1|$eventId|$target|$Date"
$plainBytes = [System.Text.Encoding]::UTF8.GetBytes($payload)

$aes = [System.Security.Cryptography.Aes]::Create()
try {
    $aes.Mode = [System.Security.Cryptography.CipherMode]::ECB
    $aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
    $aes.Key = $key
    $encryptor = $aes.CreateEncryptor()
    try {
        $encrypted = $encryptor.TransformFinalBlock($plainBytes, 0, $plainBytes.Length)
    } finally {
        $encryptor.Dispose()
    }
} finally {
    $aes.Dispose()
}

$signal = "DGLAB$" + [Convert]::ToBase64String($encrypted).TrimEnd('=').Replace('+', '-').Replace('/', '_')

if ($SignalOnly) {
    Write-Output $signal
    exit 0
}

Write-Output "Date: $Date"
Write-Output "Target: $target"
Write-Output "Trigger: $eventId"
Write-Output ("Signal: " + $signal)
