# QuickBank

O QuickBank é um processo Spring Boot 3.5 em Java 21. Ele simula um banco pequeno: um cliente autenticado abre uma conta, vê caixa e crédito como números separados, deposita caixa, transfere para outra conta e lê um extrato. Os módulos compartilham o processo e o banco MySQL. Eles não chamam o serviço uns dos outros e não leem as tabelas uns dos outros. Uma escrita que outros módulos precisam ouvir é gravada junto com uma linha de outbox; um relay publica essa linha no Kafka.

Pagamento e e-mail são mocks. Não há provedor de pagamento externo e não há SMTP.

## Mapa dos módulos

| Módulo | Dono | Publica | Consome |
| --- | --- | --- | --- |
| `quickbank-bootstrap` | Segurança, subida do Flyway, beans de tópico, o jar executável | — | — |
| `quickbank-common` | Tipo de dinheiro, envelope de evento, relay do outbox, problem+json | — | — |
| `quickbank-account` | Tabelas `acc_*`: a conta, o snapshot de caixa/crédito, o extrato | `quickbank.account.events`, `quickbank.notification.commands` | `quickbank.ledger.events` |
| `quickbank-ledger` | Tabelas `led_*`: saldos dos clientes, o cofre de caixa, journals, depósitos, transferências | `quickbank.payment.commands`, `quickbank.ledger.events` | `quickbank.account.events`, `quickbank.payment.events` |
| `quickbank-payment` | `pay_inbox`, `pay_outbox` | `quickbank.payment.events` | `quickbank.payment.commands` |
| `quickbank-notification` | `ntf_inbox` | — | `quickbank.notification.commands` |
| `quickbank-observability` | Laboratório de spans em memória | Traces e métricas OTLP | — |

`quickbank-common` é uma biblioteca, não um artefato executável. O bootstrap é o único módulo com plugin do Spring Boot. Um teste ArchUnit recusa import de um módulo de domínio para outro.

Dinheiro é `DECIMAL(19,2)` no MySQL e `BigDecimal` no Java. Não há valor `float` nem `double` em nenhum ponto do caminho de escrita.

## Fluxos

Os tópicos têm uma partição, então os consumidores veem os eventos na ordem de publicação. O envelope é JSON com `eventId`, `eventType`, `eventVersion` (sempre 1), `occurredAt`, `idempotencyKey` e `payload`. Valores dentro do payload são strings.

**Abrir conta.** `POST /api/accounts` ou o botão em `/account`. O módulo de conta aloca um número de 8 dígitos, insere a conta e um snapshot (caixa 0.00, limite de crédito 2000.00, crédito usado 0.00) e grava duas linhas de outbox nessa transação: `AccountOpened` em `quickbank.account.events` e `SendEmail` (`ACCOUNT_OPENED`) em `quickbank.notification.commands`. O ledger consome `AccountOpened` e insere uma conta contábil do cliente com caixa 0.00. O módulo de notificação registra o e-mail no log. Nada é criado antes para ana ou bruno.

**Depósito.** `POST /api/accounts/me/deposits` com o header `Idempotency-Key`, ou o formulário em `/account`. O ledger grava o depósito como `PENDING`: o pedido ficou registrado e o caixa ainda não se moveu. Na mesma transação ele escreve `PaymentRequested` em `quickbank.payment.commands`, o tópico que pede a cobrança do dinheiro. A API devolve 202. 202 quer dizer que o pedido foi aceito e o processamento ainda não terminou; o caixa não muda nessa resposta. Quem tiraria o dinheiro de fato seria um PSP (Payment Service Provider): a parte externa que executaria a cobrança, como um adquirente de cartão ou um provedor de Pix. O QuickBank não chama um. O módulo de pagamento é um mock: ele espera (padrão 750ms), registra no log que nenhum PSP foi chamado e escreve `PaymentCompleted` com status `SUCCEEDED`. O ledger consome isso, trava a conta do cliente e a conta `VAULT` e lança um journal equilibrado: débito em `VAULT`, crédito no cliente. Em seguida escreve `DepositPosted` em `quickbank.ledger.events`. O módulo de conta ajusta o caixa do snapshot para o saldo corrente do evento, acrescenta uma linha no extrato e escreve `SendEmail` (`DEPOSIT_POSTED`). Repetir a mesma chave de idempotência devolve o depósito original e não lança duas vezes.

**Transferência.** `POST /api/transfers` com `Idempotency-Key`. O ledger trava as duas contas de cliente na ordem do número da conta, recusa transferência para a mesma conta, recusa destino desconhecido e recusa a chamada quando o caixa de origem é menor que o valor. Caso contrário, debita a origem e credita o destino numa transação só e escreve um evento `TransferPosted`. O módulo de conta aplica as duas linhas de extrato e enfileira `TRANSFER_SENT` e `TRANSFER_RECEIVED`. As colunas de crédito não são tocadas. Transferência não passa pelo mock de pagamento; depósito passa.

**Extrato.** `GET /api/accounts/me/statement` ou `/account/statement`. O módulo de conta lê a própria tabela de extrato, ordenada por tempo e depois por id. Cada linha tem tipo (`DEPOSIT`, `TRANSFER_OUT`, `TRANSFER_IN`), valor, contraparte e o saldo de caixa depois do movimento. Os horários são gravados em UTC e mostrados em America/Sao_Paulo.

## Autenticação

Realm Keycloak `quickbank`, importado de `docker/keycloak/quickbank-realm.json` na subida.

| | |
| --- | --- |
| Cliente | `quickbank-web`, confidencial, segredo `quickbank-web-secret` |
| Grants | Authorization code para a interface. Direct access (password) para curl. Esse grant existe só nesta demonstração. |
| Papel | Papel de realm `CLIENT` |
| Usuários | `ana` / `ana` (`ana@quickbank.local`), `bruno` / `bruno` (`bruno@quickbank.local`) |
| Console de administração | `admin` / `admin` em http://localhost:8080/admin |

A interface server-rendered é um cliente OAuth2 authorization code. Entrar a partir de `/` vai ao Keycloak e volta para `/account`. A API é um resource server: JWT sem sessão, `Authorization: Bearer`. Chamadas do navegador e chamadas da API usam cadeias de filtros de segurança separadas. O issuer no token é `http://localhost:8080/realms/quickbank`. Dentro do Compose a aplicação carrega as chaves de `http://keycloak:8080/.../certs` para não precisar chamar o próprio localhost.

`POST /api/auth/registrations` cria o usuário no Keycloak com o papel `CLIENT` e grava a linha em `id_registrations` (usuário, e-mail, nome, sobrenome, id do Keycloak). Nome e sobrenome são obrigatórios: o perfil do realm recusa o token sem eles. A senha não fica no MySQL. E-mail já existente responde 409 `email-already-registered`. Usuário já existente responde 409 `registration-rejected`, com o mesmo texto genérico e sem dizer que o usuário existe. `POST /api/auth/login` usa o grant de senha do cliente `quickbank-web` e, se falhar, responde 401 `sign-in-failed` com um corpo genérico. Os spans são `quickbank.identity.register` e `quickbank.identity.sign-in`. Os logs são `registration accepted`, `registration rejected reason=...` e `sign-in accepted` / `sign-in rejected`. A senha não entra no log.

```bash
curl -s -X POST http://localhost:8088/api/auth/registrations \
  -H 'Content-Type: application/json' \
  -d '{"username":"carol","email":"carol@quickbank.local","firstName":"Carol","lastName":"Lima","password":"carol-pass"}'
```

Token para curl:

```bash
TOKEN=$(curl -s -X POST 'http://localhost:8080/realms/quickbank/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=quickbank-web' \
  -d 'client_secret=quickbank-web-secret' \
  -d 'username=ana' \
  -d 'password=ana' \
  -d 'grant_type=password' \
  -d 'scope=openid profile email' | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

curl -s -X POST http://localhost:8088/api/accounts -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8088/api/accounts/me -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8088/api/accounts/me/deposits \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: deposit-ana-1' -d '{"amount":"100.00"}'
curl -s -X POST http://localhost:8088/api/transfers \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-ana-1' \
  -d '{"toAccountNumber":"10000002","amount":"25.00"}'
curl -s http://localhost:8088/api/accounts/me/statement -H "Authorization: Bearer $TOKEN"
```

Erros são `application/problem+json`. Fundos insuficientes é 422, conta desconhecida é 404, chave de idempotência reutilizada com outro payload é 409.

## Interface Angular

`quickbank-web/` é uma aplicação Angular separada. Ela não substitui as páginas renderizadas no servidor na porta 8088. As duas falam com a mesma API.

A página inicial envia usuário e senha para `POST /api/auth/login` e o cadastro para `POST /api/auth/registrations`. A API fala com o Keycloak; o bundle não guarda o segredo de `quickbank-web`. O cliente público `quickbank-spa` continua no realm `quickbank` para authorization code com PKCE. Os usuários continuam `ana` / `ana` e `bruno` / `bruno`, com o papel de realm `CLIENT`. O issuer é `http://localhost:8080/realms/quickbank`. A aplicação envia o access token em `Authorization: Bearer`.

A URL base da API e o host do Keycloak estão em `quickbank-web/src/environments/environment.ts` (e em `environment.production.ts` no `ng build`). Os dois usam as portas que o Compose publica no host: API `http://localhost:8088`, Keycloak `http://localhost:8080`. O navegador chama essas portas direto, então os valores são os mesmos no `ng serve` e na imagem nginx.

| Tela | Chamadas |
| --- | --- |
| Entrada | `POST /api/auth/login` |
| Cadastro | `POST /api/auth/registrations` |
| Conta, ainda sem conta | `POST /api/accounts` |
| Conta | `GET /api/accounts/me`, `GET /api/accounts/me/statement` |
| Depósito | `POST /api/accounts/me/deposits` com `Idempotency-Key`, depois `GET /api/deposits/{id}` até o status ser `POSTED` |
| Transferência | `POST /api/transfers` com `Idempotency-Key` |
| Extrato | `GET /api/accounts/me` e `GET /api/accounts/me/statement` |

A API aceita chamadas de navegador da origem de desenvolvimento do Angular (`http://localhost:4200` e `http://127.0.0.1:4200`) e da origem do Compose (`http://localhost:4280` e `http://127.0.0.1:4280`). Outras origens são recusadas. GET e POST continuam exigindo um token Bearer com o papel `CLIENT`. O preflight não leva token.

Para rodar só a interface, com um Compose que já tenha Keycloak e a API:

```bash
cd quickbank-web
npm install
npm start
```

Abra http://localhost:4200. `npm run build` em `quickbank-web` gera o bundle de produção.

`docker compose up --build` também constrói `quickbank-web/Dockerfile` (build Node, depois nginx) e serve em http://localhost:4280. Recrie o Keycloak depois desta mudança para o import do realm pegar o `quickbank-spa`. Um container Keycloak que já existe pula um realm que ele já carregou:

```bash
docker compose up --build --force-recreate keycloak
```

## MySQL

Um banco, `quickbank`, InnoDB, `utf8mb4` / `utf8mb4_0900_ai_ci`. O Flyway roda o `db/migration` de cada módulo (`V10` conta, `V20` ledger, `V30` pagamento, `V40` notificação). O `ddl-auto` do Hibernate é `validate`.

Tabelas de conta: `acc_accounts`, `acc_snapshots` (caixa, limite de crédito, crédito usado), `acc_statement_entries`, `acc_number_seq`, `acc_outbox`. Tabelas do ledger: `led_accounts` (clientes mais uma linha `VAULT` semeada), `led_journals`, `led_postings`, `led_deposits`, `led_transfers`, `led_outbox`. Os índices cobrem o subject do dono, chaves de idempotência, a ordem do extrato `(account_number, occurred_at, id)` e linhas de outbox ainda não publicadas. Check constraints mantêm valores positivos e o caixa do cliente não negativo. Instantes são `DATETIME(6)` em UTC, o tipo que o Hibernate valida para `java.time.Instant` no MySQL 8. O fuso da sessão não é aplicado, ao contrário de `TIMESTAMP`.

HikariCP, nome do pool `QuickBankHikari`. Qualquer um destes pode ser trocado pelas variáveis `HIKARI_*` em `.env.example`.

| Configuração | Valor | Por quê |
| --- | --- | --- |
| `maximum-pool-size` | 10 | Requisições web, listeners do Kafka e o relay do outbox compartilham um pool. 10 cobre isso sem chegar perto do `max_connections` do MySQL (151). |
| `minimum-idle` | 2 | Duas conexões quentes evitam um handshake no primeiro clique depois de ocioso. Deixar as outras oito ociosas só ocupa threads do MySQL num laptop. |
| `connection-timeout` | 3s | Falha a requisição quando o pool está vazio, em vez de bloquear pelos 30s padrão do Hikari. |
| `idle-timeout` | 10 min | Aposenta conexões acima de `minimum-idle`. Menor que `max-lifetime`. |
| `max-lifetime` | 30 min | Recicla antes do `wait_timeout` do MySQL (8h) e antes de um NAT típico derrubar uma sessão TCP quieta. |
| `keepalive-time` | 5 min | Faz ping nas conexões ociosas para o MySQL e a rede não as matarem em silêncio. Menor que o idle timeout e que o max lifetime. |
| `leak-detection-threshold` | 20s | Registra um stack se uma conexão ficar emprestada. Transações de negócio são bem mais curtas. A espera do mock de pagamento fica fora de transação. |

`open-in-view` está desligado.

## Kafka, envelope, outbox

Broker: `apache/kafka:3.9.2`, KRaft, um nó combinado. Os tópicos são criados pelo `KafkaAdmin` do Spring na subida (`partitions=1`, replicação 1) e o auto-create também está ligado.

| Tópico | Produtores | Grupo consumidor |
| --- | --- | --- |
| `quickbank.account.events` | account | `quickbank.ledger.accounts` |
| `quickbank.payment.commands` | ledger | `quickbank.payment` |
| `quickbank.payment.events` | payment | `quickbank.ledger.payments` |
| `quickbank.ledger.events` | ledger | `quickbank.account.ledger` |
| `quickbank.notification.commands` | account | `quickbank.notification` |

Cada módulo que publica tem a própria tabela de outbox. O insert de negócio e o insert do outbox commitam juntos. Um relay agendado (`fixedDelay` 250ms) reserva linhas não publicadas com `FOR UPDATE SKIP LOCKED`, envia (`acks=all`) com os headers `event-type`, `event-id` e `idempotency-key`, e então grava `published_at`. Uma queda depois do envio e antes dessa marca publica uma duplicata; os consumidores tratam idempotência por `eventId` ou pela chave de idempotência de negócio.

A aplicação dentro do Compose usa `kafka:19092`. Um consumidor no host usa `localhost:9092`.

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:19092 \
  --topic quickbank.ledger.events \
  --from-beginning
```

## Mocks

**Pagamento** (`com.quickbank.mock.payment`). Consome `PaymentRequested`. Se `pay_inbox` já tem esse id de pagamento, registra no log e retorna. Senão, espera, registra `MOCK payment — no PSP was called`, insere a linha do inbox e publica `PaymentCompleted` / `SUCCEEDED` pelo próprio outbox. Sempre sucede. Depósitos ficam `PENDING` se esse consumidor estiver fora.

**E-mail** (`com.quickbank.mock.notification`). Consome `SendEmail`. Insere em `ntf_inbox` (a chave primária é o id do evento) e registra `MOCK email — no SMTP delivery` com destinatário, template, assunto e corpo. Templates: `ACCOUNT_OPENED`, `DEPOSIT_POSTED`, `TRANSFER_SENT`, `TRANSFER_RECEIVED`. Um id de evento duplicado é registrado uma vez.

## Dynatrace

O tracing do Micrometer faz a ponte para OpenTelemetry e exporta OTLP. Spans e timers usam os nomes de `dynatrace/QUERIES.md`. Chaves de baixa cardinalidade (`module`, `mock`, `movement.type`) são tags de métrica. `account.number` e `transfer.id` são só atributos de span.

* Caminho local, o padrão. `DT_OTLP_ENDPOINT` vazio. A aplicação envia para `http://otel-collector:4318`. O collector encaminha os traces ao Jaeger. Interface: http://localhost:16686 (serviço `quickbank`). O laboratório em http://localhost:8088/lab lista o catálogo de spans, o DQL e os últimos spans capturados neste processo. Não precisa de tenant.
* Caminho de tenant. Defina `DT_OTLP_ENDPOINT` como `https://{env}.live.dynatrace.com/api/v2/otlp`, `DT_API_TOKEN` como um token que possa ingerir traces e métricas OpenTelemetry, e `DT_TENANT` como o id do ambiente (mostrado no laboratório, não enviado como header). A aplicação então usa `Authorization: Api-Token …` e acrescenta `/v1/traces` e `/v1/metrics` quando a URL base ainda não os inclui. Nesse modo o Jaeger local deixa de receber spans.

O texto de cada consulta, o que ela mostra e em quais atributos filtra: [dynatrace/QUERIES.md](dynatrace/QUERIES.md).

## Interfaces locais

A interface de um tenant Dynatrace não sobe neste Compose. Ela precisa de `DT_TENANT`, `DT_OTLP_ENDPOINT` e `DT_API_TOKEN`. O Grafana da porta 3000 não é essa interface: ele lê os traces que o collector já envia (o Jaeger em http://localhost:16686 continua no ar) e os logs do processo da aplicação.

| Interface | URL |
| --- | --- |
| Kafka UI | http://localhost:8085 |
| Grafana | http://localhost:3000 |

No Grafana, abra o painel QuickBank. Troque `10000001` pelo número da conta. A primeira linha de cada par é Tempo (spans). A segunda é Loki (logs).

Número da conta:

```
{ span.account.number = "10000001" }
{service="app"} |= "10000001"
```

Depósito:

```
{ span.movement.type = "DEPOSIT" }
{service="app"} |~ "DEPOSIT|MOCK payment"
```

Transferência:

```
{ name = "quickbank.ledger.transfer" }
{service="app"} |~ "TRANSFER|transfer"
```

Erros:

```
{ status = error }
{service="app"} |~ "ERROR|Exception|insufficient-funds|account-exists|idempotency-conflict"
```

O painel também filtra o mock de pagamento com `{ name = "quickbank.payment.mock" && span.mock = "true" }` e `{service="app"} |= "MOCK payment"`.

## Execução

Pré-requisitos: Docker com Compose v2. Java 21 e Maven 3.9 só são necessários para testes ou para o jar fora do Docker.

```bash
cp .env.example .env
docker compose up --build
```

| Serviço | URL ou porta no host |
| --- | --- |
| Interface server-rendered e API do QuickBank | http://localhost:8088 |
| Interface Angular do QuickBank (Compose) | http://localhost:4280 |
| Interface Angular do QuickBank (`ng serve`) | http://localhost:4200 |
| Keycloak | http://localhost:8080 |
| Kafka UI | http://localhost:8085 |
| Grafana (traces e logs; não é o Dynatrace) | http://localhost:3000 |
| Jaeger | http://localhost:16686 |
| MySQL | localhost:3306 (`quickbank` / `quickbank`) |
| Kafka a partir do host | localhost:9092 |
| OTLP HTTP | localhost:4318 |

O Keycloak fica na 8080 porque é a porta que o realm e os redirects do navegador usam. A aplicação fica na 8088 para as duas não dividirem porta. O servidor de desenvolvimento do Angular fica na 4200. O Compose serve a aplicação Angular já construída na 4280 para ela poder rodar ao lado do `ng serve`.

O mesmo passo a passo funciona em http://localhost:4280 e, com `npm start` dentro de `quickbank-web`, em http://localhost:4200. Entre no Keycloak como `ana` / `ana` ou `bruno` / `bruno`. Se o login disser que o cliente é desconhecido, recrie o container do Keycloak para ele importar `quickbank-spa`.

Passo a passo:

1. Abra http://localhost:8088 e entre como `ana` / `ana`.
2. Abra uma conta. O caixa é 0. O limite de crédito é 2.000. O crédito usado é 0. O crédito disponível é 2.000. A linha de log começa com `MOCK email`.
3. Deposite `100.00`. A página de status atualiza até o mock de pagamento terminar. Volte à conta; o caixa é 100. O extrato tem uma linha de depósito.
4. Num segundo perfil de navegador, entre como `bruno` / `bruno` e abra uma conta. Copie o número da conta.
5. Como ana, transfira um valor maior que o caixa. A página mostra a mensagem de fundos insuficientes e a API devolve 422 problem+json. Transfira para o próprio número da ana e a página diz que uma autotransferência foi recusada. Transfira `25.00` para o bruno. Os dois extratos atualizam. Os logs contêm `TRANSFER_SENT` e `TRANSFER_RECEIVED`.
6. Abra http://localhost:8088/lab e http://localhost:16686.

`docker compose down -v` apaga o volume do MySQL. A subida seguinte importa o realm do Keycloak de novo só quando o container do Keycloak é recriado; um container que já existe pula um realm que já está lá.

Para rodar o jar no host em vez de no Compose, suba MySQL, Kafka e Keycloak você mesmo e use os valores de localhost em `.env.example`.

## Testes

```bash
mvn -q verify
```

Roda os testes de unidade e empacota o jar do Boot. Sem Docker. Casos cobertos: journals de depósito e de transferência fecham, caixa insuficiente não lança, transferência para si é recusada, destino desconhecido é recusado, repetir uma transferência ou um resultado de pagamento não move o caixa duas vezes, chave de idempotência reutilizada com outro valor conflita, linhas de extrato ordenam por tempo e depois por id, um depósito não muda o crédito usado, dinheiro recusa mais de duas casas decimais, problem+json para fundos insuficientes, e fronteiras entre módulos.

`LedgerJpaRulesIT` repete as regras do ledger contra MySQL via Testcontainers. O Surefire não o executa. Com Docker:

```bash
mvn -q verify -Pdocker-tests
```

## Layout

```
pom.xml                      reactor
quickbank-common               dinheiro, eventos, outbox, erros
quickbank-account              abrir conta, snapshot, extrato, páginas da interface
quickbank-ledger               política de lançamento, depósitos, transferências
quickbank-payment              consumidor mock de PSP
quickbank-notification         consumidor mock de e-mail
quickbank-observability        roteamento OTLP e o laboratório Dynatrace
quickbank-bootstrap            aplicação, segurança, application.yml
quickbank-web                interface Angular (aplicação separada)
docker-compose.yml
Dockerfile
docker/keycloak              import do realm
docker/otel                  configuração do collector
dynatrace/QUERIES.md
.env.example
```

A interface renderizada no servidor é Thymeleaf, no mesmo processo da API. Os formulários fazem POST no módulo dono da escrita e redirecionam de volta à página da conta. O layout estreito empilha os números de saldo e transforma o extrato em linhas. A aplicação Angular em `quickbank-web/` é uma segunda interface, na própria origem. Ela chama a API REST e deixa aquelas páginas no lugar.
