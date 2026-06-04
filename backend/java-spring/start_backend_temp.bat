@echo off 
set APP_AI_MOCK_ENABLED=true 
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe" -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev 
