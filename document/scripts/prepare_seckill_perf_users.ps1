param(
    [int]$Count = 5000,
    [string]$CsvPath = "document\jmeter\seckill-members.csv"
)

$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$csvFullPath = Join-Path $repo $CsvPath
$csvDir = Split-Path -Parent $csvFullPath
New-Item -ItemType Directory -Path $csvDir -Force | Out-Null

$passwordHash = mysql -uroot -proot -D mall -N -B -e "SELECT password FROM ums_member WHERE username='test' LIMIT 1"
if ([string]::IsNullOrWhiteSpace($passwordHash)) {
    throw "Cannot read password hash from member 'test'"
}

$existingCount = [int](mysql -uroot -proot -D mall -N -B -e "SELECT COUNT(*) FROM ums_member WHERE username LIKE 'seckill_user_%'")
if ($existingCount -lt $Count) {
    $batchSize = 500
    for ($offset = 1; $offset -le $Count; $offset += $batchSize) {
        $end = [Math]::Min($offset + $batchSize - 1, $Count)
        $values = New-Object System.Collections.Generic.List[string]
        for ($i = $offset; $i -le $end; $i++) {
            $username = "seckill_user_{0:D5}" -f $i
            $phone = "199{0:D8}" -f $i
            $values.Add("('$username','$passwordHash','$username','$phone',4,1,NOW(),0,0)")
        }
        $sql = "INSERT IGNORE INTO ums_member (username,password,nickname,phone,member_level_id,status,create_time,integration,growth) VALUES " + ($values -join ",")
        $sql | mysql -uroot -proot -D mall | Out-Null
    }
}

$csvReady = $false
if (Test-Path $csvFullPath) {
    try {
        $csvReady = ((Get-Content -Path $csvFullPath -ErrorAction Stop | Measure-Object -Line).Lines -ge ($Count + 1))
    } catch {
        $csvReady = $false
    }
}

if (-not $csvReady) {
    $tmpCsvPath = "$csvFullPath.tmp"
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("username,password")
    for ($i = 1; $i -le $Count; $i++) {
        $lines.Add(("seckill_user_{0:D5},123456" -f $i))
    }

    [System.IO.File]::WriteAllLines($tmpCsvPath, $lines, [System.Text.Encoding]::UTF8)
    Move-Item -LiteralPath $tmpCsvPath -Destination $csvFullPath -Force
}

Write-Output "Prepared $Count seckill members and CSV: $csvFullPath"
