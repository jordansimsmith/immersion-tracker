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

        Update-Remote-Shows $Username $Password

        Get-Remote-Progress

        Delete-Local-Episodes-Watched
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
        if (!($Episodes)) {
            Write-Host "No local episodes watched, skipping"
            return
        }

        Write-Host "Syncing $($Episodes.Count) local episodes watched..."

        $SyncMessages = @()
        $Episodes | 
        ForEach-Object {
            $SyncMessage = @{
                folder_name = $_.FolderName
                file_name   = $_.FileName
            }
            $SyncMessages += $SyncMessage
        }

        $Authorization = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f $Username, $Password)))
        $Headers = @{
            "Authorization" = "Basic " + $Authorization
            "Content-Type"  = "application/json;charset=UTF-8"
        }
        $Json = ConvertTo-Json $SyncMessages
        $Body = [System.Text.Encoding]::UTF8.GetBytes($Json)

        $Sync = Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/sync" -Method Post -Headers $Headers -Body $Body
        Write-Host "Successfully synced $($Sync.episodes_added) new episodes with the remote server"
    }
}

function Update-Remote-Shows {
    [CmdletBinding()]
    [OutputType([void])]
    param(
        [string] $Username,
        [string] $Password
    )
    process {
        Write-Host "Checking for show metadata updates..."

        $Shows = Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/shows"
        $Shows | 
        ForEach-Object {
            if ($_.tvdb_id) {
                return
            }

            [int]$id = Read-Host "Enter the TVDB id for show in folder: $($_.folder_name)"

            $Authorization = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f $Username, $Password)))
            $Headers = @{
                "Authorization" = "Basic " + $Authorization
                "Content-Type"  = "application/json"
            }
            $Body = @{
                tvdb_id = $id
            } | ConvertTo-Json
            Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/shows/$($_.id)" -Method Put -Headers $Headers -Body $Body

            Write-Host "Successfully updated show metadata"
        }
    }
}

function Get-Remote-Progress {
    [CmdletBinding()]
    param()
    process {
        Write-Host "Retrieving progress summary..."
        Write-Host

        $Progress = Invoke-RestMethod -Uri "https://immersion-tracker.jordansimsmith.com/progress"
        $Progress.shows |
        Foreach-Object {
            $Name = if ($_.name) { $_.name } else { 'Unknown' }
            Write-Host "$($_.episodes_watched) episodes of $Name"
        }

        Write-Host -ForegroundColor Green "$($Progress.episodes_watched_today) episodes watched today"
        Write-Host -ForegroundColor Green "$($Progress.total_hours_watched) total hours watched"
    }
}

function Delete-Local-Episodes-Watched() {
    [CmdletBinding()]
    param()
    process {
        Write-Host
        Write-Host "Checking for watched episodes to delete..."
        $Size = 0

        Get-ChildItem -Directory |
        ForEach-Object {
            $WatchedPath = Join-Path -Path $_.FullName -ChildPath 'watched'

            if (!(Test-Path -LiteralPath $WatchedPath)) {
                return
            }

            Get-ChildItem -LiteralPath $WatchedPath | 
            ForEach-Object {
                try {
                    Remove-Item -LiteralPath $_.FullName
                    $Size += $_.Length
                }
                catch {
                    # inaccessible file, probably in use, skip
                }
            }
        }

        if ($Size) {
            $GigaBytes = "{0:n2}" -f ($Size / 1GB)
            Write-Host "$GigaBytes GB of watched episodes were deleted"
        }
    }
}