$ErrorActionPreference = [System.Management.Automation.ActionPreference]::Stop

function Sync-Episodes-Watched {
    [CmdletBinding(PositionalBinding = $false)]
    [OutputType([void])]
    param (
        [Parameter(mandatory = $true)][string] $Username,
        [Parameter(mandatory = $true)][string] $Password
    )
    process {
        $LocalEpisodesWatched = Get-Local-Episodes-Watched

        Sync-Local-Episodes-Watched $LocalEpisodesWatched $Username $Password

        Get-Remote-Episodes-Watched
    }
}

function Get-Local-Episodes-Watched {
    [CmdletBinding()]
    [OutputType([object[]])]
    param()
    process {
        Write-Host "Scanning for episodes watched locally..."

        $Episodes = @()

        Get-ChildItem -Directory |
        ForEach-Object {
            $FolderName = $_.BaseName
            $WatchedPath = Join-Path -Path $_.FullName -ChildPath 'watched'

            if (!(Test-Path -LiteralPath $WatchedPath)) {
                return
            }

            Get-ChildItem -LiteralPath $WatchedPath | 
            ForEach-Object {
                $FileName = $_.BaseName
                $Episode = @{
                    FolderName = $FolderName
                    FileName   = $FileName
                }
                $Episodes += $Episode
            }
        }

        return $Episodes
    }
}

function Sync-Local-Episodes-Watched {
    [CmdletBinding()]
    [OutputType([void])]
    param(
        [object[]] $Episodes,
        [string] $Username,
        [string] $Password
    )
    process {
        Write-Host "Syncing $($Episodes.Count) local episodes watched..."

        $Now = (Get-Date).ToString("yyyy-MM-ddThh:mm:ss")
        $SyncMessages = @()
        $Episodes | 
        ForEach-Object {
            $SyncMessage = @{
                folder_name = $_.FolderName
                file_name   = $_.FileName
                timestamp   = $Now
            }
            $SyncMessages += $SyncMessage
        }

        $Authorization = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f $Username, $Password)))
        $Headers = @{
            "Authorization" = "Basic " + $Authorization
            "Content-Type"  = "application/json"
        }
        $Body = $SyncMessages | ConvertTo-Json

        $Sync = Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/sync" -Method Post -Headers $Headers -Body $Body
        Write-Host "Successfully synced $($Sync.episodes_added) new episodes with the remote server"
    }
}

function Get-Remote-Episodes-Watched {
    [CmdletBinding()]
    param()
    process {
        Write-Host "Retrieving progress summary..."
        Write-Host

        $Progress = Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/shows"
        $Progress.shows |
        Foreach-Object {
            $Name = if ($_.tvdb_name) { $.tvdb_name } else { $.folder_name }
            Write-Host "$($_.episodes_watched) episodes of $Name"
        }

        Write-Host
        Write-Host "$($Progress.total_hours_watched) total hours watched"
    }
}