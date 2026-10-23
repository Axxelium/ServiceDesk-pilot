param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'

function Invoke-DeskRequest {
    param([string]$Method, [string]$Path, [object]$Body)
    $request = @{ Method = $Method; Uri = "$BaseUrl/api/v1$Path" }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json))
    }
    Invoke-RestMethod @request
}

$suffix = [guid]::NewGuid().ToString('N').Substring(0, 8)
$engineer = Invoke-DeskRequest POST '/engineers' @{ name = 'Dana'; email = "dana-$suffix@example.com" }
$ticket = Invoke-DeskRequest POST '/tickets' @{
    title = 'VPN connection fails'
    description = 'Certificate expired after an update'
    requesterEmail = 'employee@example.com'
}
$id = $ticket.id
Invoke-DeskRequest PUT "/tickets/$id/assignee" @{ engineerId = $engineer.id } | Out-Null
Invoke-DeskRequest PATCH "/tickets/$id/status" @{ status = 'IN_PROGRESS' } | Out-Null
Invoke-DeskRequest POST "/tickets/$id/comments" @{
    author = 'Dana'; body = 'Renewed certificate and verified VPN connection'
} | Out-Null
Invoke-DeskRequest PATCH "/tickets/$id/status" @{ status = 'RESOLVED' } | Format-List
Invoke-DeskRequest GET "/tickets/$id/comments" | ConvertTo-Json -Depth 5
Write-Host "Demo completed. Ticket ID: $id"
