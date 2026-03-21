$loginBody = @{ username = "admin"; password = "password" } | ConvertTo-Json
$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
Write-Host "Token acquired: $($loginResponse.token.Substring(0,20))..."

$companyBody = @{
    name = "TestCo"
    gstNumber = "22BBBBB0001A1Z1"
    billingEmail = "test@example.com"
    serviceChargePct = 5
    billingCycle = "MONTHLY"
} | ConvertTo-Json

try {
    $headers = @{ Authorization = "Bearer $($loginResponse.token)" }
    $result = Invoke-RestMethod -Uri "http://localhost:8080/api/companies" -Method POST -ContentType "application/json" -Body $companyBody -Headers $headers
    Write-Host "SUCCESS: Company created with ID $($result.id)"
} catch {
    Write-Host "ERROR STATUS: $($_.Exception.Response.StatusCode.value__)"
    $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
    $errorBody = $reader.ReadToEnd()
    Write-Host "ERROR BODY: $errorBody"
}
