# P0 case workflow API regression (ASCII-only script; usernames from DB)
$ErrorActionPreference = 'Stop'
$Base = 'http://localhost:8080'
$Pwd = 'admin123'

$users = Get-Content (Join-Path $PSScriptRoot 'p0-users.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$UAcceptor1 = $users.acceptor1
$UAcceptor2 = $users.acceptor2
$UDispatcher1 = $users.dispatcher1
$UDept = $users.dept
$UHandler = $users.handler
$UCollector = $users.collector

Write-Host "Accounts: acceptor1=$UAcceptor1 dispatcher=$UDispatcher1 dept=$UDept handler=$UHandler"

function Login([string]$User) {
  $body = @{ username = $User; password = $Pwd } | ConvertTo-Json
  $r = Invoke-RestMethod -Uri "$Base/auth/login" -Method Post `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -ContentType 'application/json; charset=utf-8'
  if ($r.code -ne 200) { throw "login failed [$User]: $($r.message)" }
  return $r.data.token
}

function Invoke-CaseApi {
  param([string]$Token, [string]$Method, [string]$Path, $Body = $null)
  $h = @{ Authorization = "Bearer $Token" }
  $uri = "$Base$Path"
  if ($null -ne $Body) {
    $json = $Body | ConvertTo-Json -Depth 8 -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    return Invoke-RestMethod -Uri $uri -Method $Method -Headers $h `
      -Body $bytes -ContentType 'application/json; charset=utf-8'
  }
  return Invoke-RestMethod -Uri $uri -Method $Method -Headers $h
}

function Assert-Ok($r, [string]$step) {
  if ($r.code -ne 200) { throw "${step}: $($r.message)" }
  return $r.data
}

function Get-CaseDetail($token, $id) {
  Assert-Ok (Invoke-CaseApi -Token $token -Method Get -Path "/case/$id") "get $id"
}

$results = @()
$mainCaseId = $null
$guardCaseId = $null

function Run-Step([string]$name, [scriptblock]$block) {
  Write-Host "`n== $name ==" -ForegroundColor Cyan
  try {
    & $block
    $script:results += [pscustomobject]@{ Step = $name; Result = 'PASS' }
    Write-Host 'PASS' -ForegroundColor Green
  } catch {
    $script:results += [pscustomobject]@{ Step = $name; Result = "FAIL: $($_.Exception.Message)" }
    Write-Host "FAIL: $($_.Exception.Message)" -ForegroundColor Red
  }
}

function New-Report($token) {
  $report = @{
    reporterId = 19
    categoryType = 'event'
    bigCode = '01'
    bigName = '市容环境'
    smallCode = '99'
    smallName = '测试小类（联调）'
    smallId = 366
    standardId = 1257
    address = 'P0 test'
    longitude = 111.0
    latitude = 35.0
    description = 'P0 regression'
    attachments = @()
  }
  Assert-Ok (Invoke-CaseApi -Token $token -Method Post -Path '/case/report' -Body $report) 'report'
}

Run-Step 'A1 collector report' {
  $t = Login $UCollector
  $c = New-Report $t
  $script:mainCaseId = $c.id
  if ($c.caseStatus -ne 'pending_register') { throw "status=$($c.caseStatus)" }
}

Run-Step 'A2 acceptor register' {
  $t = Login $UAcceptor1
  $d = Get-CaseDetail $t $script:mainCaseId
  $body = @{
    caseId = $script:mainCaseId
    dispatcherUserId = 23
    standardId = 1257
    clientUpdateTime = $d.updateTime
  }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/register' -Body $body) 'register'
  if ($c.caseStatus -ne 'pending_dispatch') { throw "status=$($c.caseStatus)" }
}

Run-Step 'A3 dispatcher dispatch' {
  $t = Login $UDispatcher1
  $d = Get-CaseDetail $t $script:mainCaseId
  $body = @{ caseId = $script:mainCaseId; departmentId = 100; clientUpdateTime = $d.updateTime }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/dispatch' -Body $body) 'dispatch'
  if ($c.caseStatus -ne 'pending_handle') { throw "status=$($c.caseStatus)" }
}

Run-Step 'A4 dept assign handler' {
  $t = Login $UDept
  $d = Get-CaseDetail $t $script:mainCaseId
  $body = @{ caseId = $script:mainCaseId; handlerUserId = 33; clientUpdateTime = $d.updateTime }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/assign-handler' -Body $body) 'assign'
  if ($c.caseStatus -ne 'handling') { throw "status=$($c.caseStatus)" }
}

Run-Step 'A5 handler submit' {
  $t = Login $UHandler
  $body = @{ caseId = $script:mainCaseId; remark = 'done'; attachments = @() }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/handle' -Body $body) 'handle'
  if ($c.caseStatus -ne 'handle_finish') { throw "status=$($c.caseStatus)" }
}

Run-Step 'A6 dept confirm' {
  $t = Login $UDept
  $d = Get-CaseDetail $t $script:mainCaseId
  $body = @{ caseId = $script:mainCaseId; dispatcherUserId = 23; clientUpdateTime = $d.updateTime }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/dept-confirm' -Body $body) 'dept-confirm'
  if ($c.caseStatus -ne 'pending_check') { throw "status=$($c.caseStatus)" }
}

Run-Step 'A7 dispatcher forward acceptor' {
  $t = Login $UDispatcher1
  $d = Get-CaseDetail $t $script:mainCaseId
  $body = @{ caseId = $script:mainCaseId; acceptorUserId = 34; clientUpdateTime = $d.updateTime }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/dispatcher-forward-acceptor' -Body $body) 'forward'
  if ([string]$c.currentHandlerId -ne '34') { throw "handler=$($c.currentHandlerId)" }
}

Run-Step 'A8 acceptor close' {
  $t = Login $UAcceptor2
  $body = @{ caseId = $script:mainCaseId; remark = 'close' }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/close' -Body $body) 'close'
  if ($c.caseStatus -ne 'closed') { throw "status=$($c.caseStatus)" }
}

Run-Step 'B1 setup guard case' {
  $tc = Login $UCollector
  $c = New-Report $tc
  $script:guardCaseId = $c.id
  $ta = Login $UAcceptor1
  $d = Get-CaseDetail $ta $script:guardCaseId
  Assert-Ok (Invoke-CaseApi -Token $ta -Method Post -Path '/case/register' -Body @{
    caseId = $script:guardCaseId; dispatcherUserId = 23; standardId = 1257; clientUpdateTime = $d.updateTime
  }) 'reg' | Out-Null
  $td = Login $UDispatcher1
  $d2 = Get-CaseDetail $td $script:guardCaseId
  Assert-Ok (Invoke-CaseApi -Token $td -Method Post -Path '/case/dispatch' -Body @{
    caseId = $script:guardCaseId; departmentId = 100; clientUpdateTime = $d2.updateTime
  }) 'disp' | Out-Null
}

Run-Step 'B2 dept return' {
  $t = Login $UDept
  $d = Get-CaseDetail $t $script:guardCaseId
  $body = @{ caseId = $script:guardCaseId; remark = 'wrong dept'; clientUpdateTime = $d.updateTime }
  $c = Assert-Ok (Invoke-CaseApi -Token $t -Method Post -Path '/case/dept-return' -Body $body) 'dept-return'
  if ($c.caseStatus -ne 'returned') { throw "status=$($c.caseStatus)" }
}

Run-Step 'B3 guard forward rejected' {
  $t = Login $UDispatcher1
  $d = Get-CaseDetail $t $script:guardCaseId
  $body = @{ caseId = $script:guardCaseId; acceptorUserId = 34; clientUpdateTime = $d.updateTime }
  $r = Invoke-CaseApi -Token $t -Method Post -Path '/case/dispatcher-forward-acceptor' -Body $body
  if ($r.code -eq 200) { throw 'forward should fail after dept return' }
  Write-Host "  expected: $($r.message)" -ForegroundColor DarkYellow
}

Run-Step 'C1 dispatcher return acceptor' {
  $tc = Login $UCollector
  $c = New-Report $tc
  $id = $c.id
  $ta = Login $UAcceptor1
  $d = Get-CaseDetail $ta $id
  Assert-Ok (Invoke-CaseApi -Token $ta -Method Post -Path '/case/register' -Body @{
    caseId = $id; dispatcherUserId = 23; standardId = 1257; clientUpdateTime = $d.updateTime
  }) 'reg' | Out-Null
  $td = Login $UDispatcher1
  $d2 = Get-CaseDetail $td $id
  $c2 = Assert-Ok (Invoke-CaseApi -Token $td -Method Post -Path '/case/dispatcher-return-acceptor' -Body @{
    caseId = $id; acceptorUserId = 22; remark = 'not bureau'; clientUpdateTime = $d2.updateTime
  }) 'ret-acc'
  if ($c2.caseStatus -ne 'returned') { throw "status=$($c2.caseStatus)" }
}

function Setup-ToHandling {
  $tc = Login $UCollector
  $c = New-Report $tc
  $id = $c.id
  $ta = Login $UAcceptor1
  $d = Get-CaseDetail $ta $id
  Assert-Ok (Invoke-CaseApi -Token $ta -Method Post -Path '/case/register' -Body @{
    caseId = $id; dispatcherUserId = 23; standardId = 1257; clientUpdateTime = $d.updateTime
  }) 'reg' | Out-Null
  $td = Login $UDispatcher1
  $d2 = Get-CaseDetail $td $id
  Assert-Ok (Invoke-CaseApi -Token $td -Method Post -Path '/case/dispatch' -Body @{
    caseId = $id; departmentId = 100; clientUpdateTime = $d2.updateTime
  }) 'disp' | Out-Null
  $tdept = Login $UDept
  $d3 = Get-CaseDetail $tdept $id
  Assert-Ok (Invoke-CaseApi -Token $tdept -Method Post -Path '/case/assign-handler' -Body @{
    caseId = $id; handlerUserId = 33; clientUpdateTime = $d3.updateTime
  }) 'assign' | Out-Null
  return $id
}

Run-Step 'D1 handler return dept' {
  $id = Setup-ToHandling
  $th = Login $UHandler
  $d = Get-CaseDetail $th $id
  $c = Assert-Ok (Invoke-CaseApi -Token $th -Method Post -Path '/case/handler-return-dept' -Body @{
    caseId = $id; remark = 'wrong assign'; clientUpdateTime = $d.updateTime
  }) 'handler-ret'
  if ($c.caseStatus -ne 'pending_handle') { throw "status=$($c.caseStatus)" }
}

Run-Step 'D2 revoke assign then dept return' {
  $id = Setup-ToHandling
  $tdept = Login $UDept
  $d = Get-CaseDetail $tdept $id
  Assert-Ok (Invoke-CaseApi -Token $tdept -Method Post -Path '/case/dept-revoke-assign' -Body @{
    caseId = $id; remark = 'revoke'; clientUpdateTime = $d.updateTime
  }) 'revoke' | Out-Null
  $d2 = Get-CaseDetail $tdept $id
  $c = Assert-Ok (Invoke-CaseApi -Token $tdept -Method Post -Path '/case/dept-return' -Body @{
    caseId = $id; remark = 'not our unit'; clientUpdateTime = $d2.updateTime
  }) 'dept-ret'
  if ($c.caseStatus -ne 'returned') { throw "status=$($c.caseStatus)" }
}

Run-Step 'D3 dept return handler' {
  $id = Setup-ToHandling
  $th = Login $UHandler
  Assert-Ok (Invoke-CaseApi -Token $th -Method Post -Path '/case/handle' -Body @{
    caseId = $id; remark = 'done'; attachments = @()
  }) 'handle' | Out-Null
  $tdept = Login $UDept
  $d = Get-CaseDetail $tdept $id
  $c = Assert-Ok (Invoke-CaseApi -Token $tdept -Method Post -Path '/case/dept-return-handler' -Body @{
    caseId = $id; remark = 'not good enough'; clientUpdateTime = $d.updateTime
  }) 'dept-ret-h'
  if ($c.caseStatus -ne 'handling') { throw "status=$($c.caseStatus)" }
}

Run-Step 'D4 dispatcher return dept rework' {
  $id = Setup-ToHandling
  $th = Login $UHandler
  Assert-Ok (Invoke-CaseApi -Token $th -Method Post -Path '/case/handle' -Body @{
    caseId = $id; remark = 'done'; attachments = @()
  }) 'handle' | Out-Null
  $tdept = Login $UDept
  $d = Get-CaseDetail $tdept $id
  Assert-Ok (Invoke-CaseApi -Token $tdept -Method Post -Path '/case/dept-confirm' -Body @{
    caseId = $id; dispatcherUserId = 23; clientUpdateTime = $d.updateTime
  }) 'confirm' | Out-Null
  $td = Login $UDispatcher1
  $d2 = Get-CaseDetail $td $id
  $c = Assert-Ok (Invoke-CaseApi -Token $td -Method Post -Path '/case/dispatcher-return-dept' -Body @{
    caseId = $id; remark = 'rework needed'; clientUpdateTime = $d2.updateTime
  }) 'disp-ret-dept'
  if ($c.caseStatus -ne 'pending_handle') { throw "status=$($c.caseStatus)" }
}

Run-Step 'D5 acceptor return dispatcher' {
  $id = Setup-ToHandling
  $th = Login $UHandler
  Assert-Ok (Invoke-CaseApi -Token $th -Method Post -Path '/case/handle' -Body @{ caseId = $id; remark = 'd'; attachments = @() }) 'h' | Out-Null
  $tdept = Login $UDept
  $d = Get-CaseDetail $tdept $id
  Assert-Ok (Invoke-CaseApi -Token $tdept -Method Post -Path '/case/dept-confirm' -Body @{
    caseId = $id; dispatcherUserId = 23; clientUpdateTime = $d.updateTime
  }) 'c' | Out-Null
  $td = Login $UDispatcher1
  $d2 = Get-CaseDetail $td $id
  Assert-Ok (Invoke-CaseApi -Token $td -Method Post -Path '/case/dispatcher-forward-acceptor' -Body @{
    caseId = $id; acceptorUserId = 34; clientUpdateTime = $d2.updateTime
  }) 'fwd' | Out-Null
  $ta = Login $UAcceptor2
  $d3 = Get-CaseDetail $ta $id
  $c = Assert-Ok (Invoke-CaseApi -Token $ta -Method Post -Path '/case/acceptor-return-dispatcher' -Body @{
    caseId = $id; dispatcherUserId = 23; remark = 'quality fail'; clientUpdateTime = $d3.updateTime
  }) 'acc-ret'
  if ($c.caseStatus -ne 'pending_handle') { throw "status=$($c.caseStatus)" }
}

Write-Host "`n======== SUMMARY ========" -ForegroundColor Cyan
$results | Format-Table -AutoSize
$fail = @($results | Where-Object { $_.Result -ne 'PASS' }).Count
Write-Host "Failed: $fail / $($results.Count)"
if ($fail -gt 0) { exit 1 }
