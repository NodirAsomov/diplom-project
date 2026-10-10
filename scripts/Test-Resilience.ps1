param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
function Api([string]$method, [string]$url, $body = $null) {
    $requestOptions = @{Method=$method; Uri=$url; TimeoutSec=30}
    if ($null -ne $body) {
        $requestOptions.ContentType = 'application/json'
        $requestOptions.Body = $body | ConvertTo-Json -Depth 8 -Compress
    }
    Invoke-RestMethod @requestOptions
}
function Check([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
}
function Compose([string[]]$arguments) {
    & docker compose @arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose failed: $arguments" }
}
$tag = [Guid]::NewGuid().ToString('N')
$owner = Api POST "$BaseUrl/admin/users" @{name="owner-$tag"; email="owner-$tag@test.local"}
$guest = Api POST "$BaseUrl/admin/users" @{name="guest-$tag"; email="guest-$tag@test.local"}
$category = Api POST "$BaseUrl/admin/categories" @{name="resilience-$tag"}
$event = Api POST "$BaseUrl/users/$($owner.id)/events" @{
    annotation='Resilience scenario annotation'
    description='Resilience scenario description'
    eventDate=(Get-Date).AddDays(3).ToString('yyyy-MM-dd HH:mm:ss')
    category=$category.id
    location=@{lat=55.75;lon=37.61}
    title='Resilience scenario'
    participantLimit=0
    paid=$false
    requestModeration=$false
}
$event = Api PATCH "$BaseUrl/admin/events/$($event.id)" @{stateAction='PUBLISH_EVENT'}
$null = Api POST "$BaseUrl/users/$($guest.id)/requests?eventId=$($event.id)"
$null = Api POST "$BaseUrl/users/$($guest.id)/events/$($event.id)/comments" @{text='Test comment for resilience'}
$null = Api PUT "$BaseUrl/users/$($guest.id)/events/$($event.id)/like"
$compilation = Api POST "$BaseUrl/admin/compilations" @{title="Proof-$tag";pinned=$false;events=@($event.id)}
$initial = Api GET "$BaseUrl/events/$($event.id)"
Check ($initial.confirmedRequests -eq 1 -and $initial.likes -eq 1 -and $initial.commentCount -eq 1) 'Fixture counters are incorrect'
$internal = Invoke-WebRequest "$BaseUrl/internal/users/$($owner.id)" -SkipHttpErrorCheck
Check ($internal.StatusCode -eq 404) 'Gateway exposes internal API'
$results = [Collections.Generic.List[object]]::new()
$stopped = [Collections.Generic.List[string]]::new()
try {
    foreach ($service in 'request-service','feature-service','stats-server','user-service') {
        Compose -arguments @('stop',$service)
        $stopped.Add($service)
        $result = Api GET "$BaseUrl/events/$($event.id)"
        $list = @(Api GET "$BaseUrl/events?categories=$($category.id)&size=100")
        $comp = Api GET "$BaseUrl/compilations/$($compilation.id)"
        Check ($result.id -eq $event.id -and $list.id -contains $event.id -and $comp.events.id -contains $event.id) "Event unavailable after stopping $service"
        Check ($result.confirmedRequests -eq 0) 'Request fallback is not zero'
        if ($stopped.Contains('feature-service')) {
            Check ($result.likes -eq 0 -and $result.commentCount -eq 0) 'Feature fallback is not zero'
        }
        if ($stopped.Contains('stats-server')) { Check ($result.views -eq 0) 'Stats fallback is not zero' }
        $results.Add(@{stopped=$service;event=$result.id;status=200;confirmed=$result.confirmedRequests;likes=$result.likes;views=$result.views})
    }
    foreach ($service in 'gateway-server','config-server','discovery-server') {
        Compose -arguments @('stop',$service)
        $stopped.Add($service)
    }
    $direct = Api GET "http://localhost:8081/events/$($event.id)"
    Check ($direct.id -eq $event.id -and $direct.views -eq 0 -and $direct.confirmedRequests -eq 0 -and $direct.likes -eq 0) 'Event service cannot work alone'
    $results.Add(@{stopped='all applications except events';event=$direct.id;status=200})
    New-Item -ItemType Directory -Path reports -Force | Out-Null
    $results | ConvertTo-Json -Depth 5 | Set-Content reports/resilience.json -Encoding utf8
    Write-Output 'Resilience checks passed'
} finally {
    Compose -arguments @('up','-d','--wait','--wait-timeout','180')
}
