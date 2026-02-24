# BPM Order Process

Demo application for Camunda 8


## Run Job Workers

Add cluster information in `cluster.yml`.

After that, start the app from project root via

`./mvnw spring-boot:run` or
`./mvnw.cmd spring-boot:run` (Windows)

## Monitoring

The application exposes metrics via Micrometer which can be pulled by Prometheus

Start Prometheus via `docker compose up` and access the metrics at `localhost:9090`
