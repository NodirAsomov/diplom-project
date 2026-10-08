param([string]$GatewayUrl = 'http://localhost:8080', [string]$EurekaUrl = 'http://localhost:8761',
      [string]$StatsUrl = 'http://localhost:9090')
$ErrorActionPreference = 'Stop'

$registry = Invoke-RestMethod "$EurekaUrl/eureka/apps" -Headers @{Accept = 'application/json'}
$instances = @{}
foreach ($name in @('CONFIG-SERVER', 'MAIN-SERVICE', 'STATS-SERVER', 'GATEWAY-SERVER')) {
    $app = $registry.applications.application | Where-Object name -eq $name
    $instance = @($app.instance | Where-Object status -eq 'UP')[0]
    if (-not $instance) { throw "$name is not UP in Eureka; wait for registration and retry" }
    $instances[$name] = $instance
    Write-Output "$name UP at $($instance.homePageUrl)"
}
foreach ($name in @('CONFIG-SERVER', 'MAIN-SERVICE', 'STATS-SERVER')) {
    if ([int]$instances[$name].port.'$' -in @(0, 8080, 8761)) {
        throw "$name did not receive a random service port"
    }
}
$configBase = $instances['CONFIG-SERVER'].homePageUrl.TrimEnd('/')
foreach ($name in @('main-service', 'stats-server', 'gateway-server')) {
    $raw = docker compose exec -T gateway-server curl --fail --silent "$configBase/$name/default"
    if ($LASTEXITCODE -ne 0) { throw "Config Server request failed for $name" }
    $config = $raw | ConvertFrom-Json
    if ($config.name -ne $name -or @($config.propertySources).Count -ne 1) {
        throw "Unexpected configuration sources for $name"
    }
    $source = $config.propertySources[0].source
    $expectedPort = if ($name -eq 'gateway-server') { 8080 } else { 0 }
    if ($source.'server.port' -ne $expectedPort) { throw "Wrong centralized port for $name" }
    Write-Output "$name received its own centralized configuration"
}
foreach ($path in @('/categories', '/events')) {
    $response = Invoke-WebRequest "$GatewayUrl$path" -TimeoutSec 15
    if ($response.StatusCode -ne 200) { throw "Gateway request failed: $path" }
    Write-Output "$path -> HTTP 200: $($response.Content)"
}
$statsBase = $instances['STATS-SERVER'].homePageUrl.TrimEnd('/')
$raw = docker compose exec -T gateway-server curl --fail --silent "$statsBase/stats?start=2000-01-01%2000:00:00&end=2100-01-01%2000:00:00&uris=/events&unique=false"
if ($LASTEXITCODE -ne 0) { throw 'Stats API request failed' }
$stats = $raw | ConvertFrom-Json
if (-not ($stats | Where-Object { $_.uri -eq '/events' -and $_.hits -gt 0 })) {
    throw 'Stats Server did not record the /events request'
}
Write-Output "Stats Server recorded /events: $raw"
$externalStats = Invoke-RestMethod "$StatsUrl/stats?start=2000-01-01%2000:00:00&end=2100-01-01%2000:00:00&uris=/events&unique=false"
if (-not ($externalStats | Where-Object { $_.uri -eq '/events' -and $_.hits -gt 0 })) {
    throw 'Stats API is not available through the compatibility port'
}
$hit = @{app = 'infrastructure-smoke'; uri = '/infrastructure-smoke'; ip = '127.0.0.1';
         timestamp = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')} | ConvertTo-Json
$response = Invoke-WebRequest "$StatsUrl/hit" -Method Post -ContentType 'application/json' -Body $hit
if ($response.StatusCode -ne 201) { throw 'Stats /hit request failed through the compatibility port' }
Write-Output 'Stats API on external port 9090: GET /stats -> 200, POST /hit -> 201'
Write-Output 'Infrastructure smoke check passed'
