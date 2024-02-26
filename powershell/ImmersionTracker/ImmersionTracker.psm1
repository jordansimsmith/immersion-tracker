function Sync-Episodes-Watched2 {
    [CmdletBinding()]
    param (
        [Parameter(mandatory = $true)][string] $Username,
        [Parameter(mandatory = $true)][string] $Password
    )
    process {
        Write-Host "Hello $Username $Password"
    }
}