# Docker — TechMob 4.0

No application source is changed. The Java API remains on 8080; the Python simulator serves /dadosPlanta on 1880 inside the network. Nginx serves React on 5173 and proxies /api/ to Java. The real Node-RED remains external and read-only.

Set MYSQL_ROOT_PASSWORD and MYSQL_PASSWORD to distinct strong values in your shell before starting. MYSQL_USER defaults to techmob. Do not use root as MYSQL_USER. Optionally set PLANTA_URL to the actual bench's read-only endpoint; the simulator remains the fallback.

```sh
docker compose config --quiet
docker compose up -d --build
docker compose ps -a
docker compose logs -f
curl http://localhost:8080/actuator/health
curl http://localhost:5173/api/maquinas
docker compose exec db sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SHOW TABLES; SELECT COUNT(*) FROM pedido;"'
docker volume inspect toyota-techmob_mysql_data
```

Open http://localhost:5173. Wait for completed simulated orders before expecting OEE.

The canonical infra/schema.sql runs on first initialization only. Stop with docker compose down; named volumes survive. Never use down -v if you need to retain data. Back up existing databases before migrating; this setup does not import an existing host database.

Optional Python tools:
```sh
docker compose run --rm analysis data/coletor_planta.py --url http://simulator:1880/dadosPlanta --saida data/gravacoes/sessao.jsonl
docker compose run --rm analysis data/oee_calculator.py data/gravacoes/sessao.jsonl
```
Stop the collector before analyzing. Recordings and results use named volumes. Docker runtime validation must be performed on the target machine.
