@echo off
echo Setting up RMS with services (excluding Keycloak and PostgreSQL)...

REM Start services (Consul, Elasticsearch, Kafka)
echo Starting Consul, Elasticsearch, and Kafka...
docker-compose -f docker-compose-services.yml up -d

REM Wait for services to start
echo Waiting for services to start...
timeout /t 15 /nobreak

echo Services started successfully!
echo.
echo Services running:
echo - Consul: http://localhost:8500
echo - Elasticsearch: http://localhost:9200
echo - Kafka: localhost:9092
echo.
echo Your existing PostgreSQL and cloud Keycloak will be used.
echo.
echo Configure IntelliJ with environment variables from intellij-env-vars.txt
echo Then run the application!
pause