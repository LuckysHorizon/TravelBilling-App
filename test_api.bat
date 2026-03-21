@echo off
echo {"username":"admin","password":"password"} > "%TEMP%\login.json"
echo Logging in...
curl -s -c "%TEMP%\cookies.txt" -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d @"%TEMP%\login.json" > NUL

echo {"name":"TestCo","gstNumber":"22BBBBB0001A1Z1","billingEmail":"test@test.com","serviceChargePct":5,"billingCycle":"MONTHLY"} > "%TEMP%\company.json"
echo Creating company...
curl -s -w "\nHTTP_CODE: %%{http_code}\n" -b "%TEMP%\cookies.txt" -X POST http://localhost:8080/api/companies -H "Content-Type: application/json" -d @"%TEMP%\company.json"
