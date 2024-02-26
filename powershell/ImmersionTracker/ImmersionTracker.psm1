function Sync-Episodes-Watched {
    [CmdletBinding()]
    param (
        [string] $Username,
        [string] $Password
    )
    process {
        Write-Host 'Hello' + $Username + $Password
    }
}