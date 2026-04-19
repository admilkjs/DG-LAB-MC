param(
    [string]$PlayerName,

    [string]$Trigger,

    [string]$Date = (Get-Date -Format 'yyyyMMdd')
)

[Console]::InputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding

$eventOptions = @(
    [PSCustomObject]@{ Id = 'player_hurt'; Name = '受伤' }
    [PSCustomObject]@{ Id = 'player_heal'; Name = '治疗' }
    [PSCustomObject]@{ Id = 'player_death'; Name = '死亡' }
    [PSCustomObject]@{ Id = 'player_killed_by_other'; Name = '被击杀' }
    [PSCustomObject]@{ Id = 'low_health_threshold'; Name = '低血量' }
    [PSCustomObject]@{ Id = 'low_food_threshold'; Name = '低饥饿' }
    [PSCustomObject]@{ Id = 'jump'; Name = '跳跃' }
    [PSCustomObject]@{ Id = 'fall_damage'; Name = '摔落伤害' }
    [PSCustomObject]@{ Id = 'sprint_start'; Name = '开始冲刺' }
    [PSCustomObject]@{ Id = 'sneak_start'; Name = '开始潜行' }
    [PSCustomObject]@{ Id = 'shield_block'; Name = '盾牌格挡' }
    [PSCustomObject]@{ Id = 'attack_damage_all'; Name = '造成伤害（全部）' }
    [PSCustomObject]@{ Id = 'attack_damage_player'; Name = '对玩家造成伤害' }
    [PSCustomObject]@{ Id = 'attack_damage_non_player'; Name = '造成伤害（不含玩家）' }
    [PSCustomObject]@{ Id = 'attack_critical_all'; Name = '暴击（全部）' }
    [PSCustomObject]@{ Id = 'attack_critical_player'; Name = '对玩家暴击' }
    [PSCustomObject]@{ Id = 'attack_critical_non_player'; Name = '暴击（不含玩家）' }
    [PSCustomObject]@{ Id = 'attack_kill_all'; Name = '击杀（全部）' }
    [PSCustomObject]@{ Id = 'attack_kill_player'; Name = '击杀玩家' }
    [PSCustomObject]@{ Id = 'attack_kill_non_player'; Name = '击杀（不含玩家）' }
    [PSCustomObject]@{ Id = 'bow_release'; Name = '拉弓释放' }
    [PSCustomObject]@{ Id = 'totem_trigger'; Name = '不死图腾' }
    [PSCustomObject]@{ Id = 'armor_low'; Name = '护甲低耐久' }
)

function Resolve-TriggerName {
    param(
        [string]$TriggerId
    )

    $match = $eventOptions | Where-Object { $_.Id -eq $TriggerId } | Select-Object -First 1
    if ($null -ne $match) {
        return $match.Name
    }
    return $TriggerId
}

if ([string]::IsNullOrWhiteSpace($PlayerName)) {
    $PlayerName = Read-Host '请输入玩家名'
}

if ([string]::IsNullOrWhiteSpace($PlayerName)) {
    throw '玩家名不能为空。'
}

if ($Trigger -eq 'death') {
    $Trigger = 'player_death'
}

if ([string]::IsNullOrWhiteSpace($Trigger)) {
    Write-Host ''
    Write-Host '请选择要伪造的事件：'
    for ($i = 0; $i -lt $eventOptions.Count; $i++) {
        $index = $i + 1
        Write-Host ('[{0}] {1}  ({2})' -f $index, $eventOptions[$i].Name, $eventOptions[$i].Id)
    }

    while ($true) {
        $selection = Read-Host '输入序号'
        $parsedIndex = 0
        if ([int]::TryParse($selection, [ref]$parsedIndex)) {
            if ($parsedIndex -ge 1 -and $parsedIndex -le $eventOptions.Count) {
                $Trigger = $eventOptions[$parsedIndex - 1].Id
                break
            }
        }
        Write-Host '序号无效，请重新输入。'
    }
}

$signal = & "$PSScriptRoot\generate_death_signal.ps1" -PlayerName $PlayerName -Trigger $Trigger -Date $Date -SignalOnly
$normalizedPlayer = $PlayerName.Trim().ToLowerInvariant()
$triggerName = Resolve-TriggerName -TriggerId $Trigger.Trim().ToLowerInvariant()

Write-Host ''
Write-Host ('日期：{0}' -f $Date)
Write-Host ('目标玩家：{0}' -f $normalizedPlayer)
Write-Host ('事件：{0}' -f $triggerName)
Write-Host ('事件ID：{0}' -f $Trigger.Trim().ToLowerInvariant())
Write-Host ('密文：{0}' -f $signal)
