$ErrorActionPreference = "Stop"

$SyncMessages = @()
$Timestamp = (Get-Date).ToString("yyyy-MM-ddThh:mm:ss")
$Username=""
$Password=""

Get-ChildItem -Directory |
Foreach-Object {
    $FolderName = $_.BaseName
    $Path = Join-Path -Path $_.FullName -ChildPath "watched"

    if (!(Test-Path -LiteralPath $Path))
    {
        return
    }

    Get-ChildItem -LiteralPath $Path |
    Foreach-Object {
        $FileName = $_.BaseName
        $SyncMessage = @{
            folder_name = $FolderName
            file_name = $FileName
            timestamp = $Timestamp
        }
        $SyncMessages += $SyncMessage    
    }
}

$Authorization = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f $Username, $Password)))

$Headers = @{
    "Authorization" = "Basic " + $Authorization
    "Content-Type" = "application/json"
}

$Body = $SyncMessages | ConvertTo-Json

$Sync = Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/sync" -Method Post -Headers $Headers -Body $Body
Write-Host "Successfully synced" $Sync.episodes_added "new episodes with the remote server"
Write-Host

$Progress = Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/progress"
$Progress.episodes_per_show_watched.PsObject.Properties |
Foreach-Object {
    $DisplayName = $_.Name -replace "\[[^\]]*\]\s?" -replace "\([^\)]*\)\s?"
    Write-Host $_.Value "episodes of" $DisplayName
}

Write-Host
Write-Host $Progress.total_hours_watched "total hours watched"


if ($Host.Name -eq "ConsoleHost")
{
    Write-Host
    Write-Host "Press any key to continue..."
    $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
