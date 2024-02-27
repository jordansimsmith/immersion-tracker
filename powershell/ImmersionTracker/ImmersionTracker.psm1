function Sync-Episodes-Watched {
    [CmdletBinding()]
    param (
        [Parameter(mandatory = $true)][string] $Username,
        [Parameter(mandatory = $true)][string] $Password
    )
    process {
        Write-Host "Hello $Username $Password"
    }
}