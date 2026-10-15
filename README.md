# Zalura Contact Service

Microsserviço Spring Boot 3 / Java 17 / PostgreSQL para contatos por empresa.

## Desenvolvimento local

1. `docker compose up -d db`
2. `gradle bootRun` (Gradle 8.14 ou compatível).
3. `gradle test`

Porta API: 8082; PostgreSQL local: 5433. Ajuste `DB_URL`, `DB_USER` e `DB_PASSWORD` no ambiente.

## Endpoints

Todos requerem `X-Company-Id: <UUID>`:
- POST /api/contacts
- GET /api/contacts?page=0&size=20
- GET /api/contacts/{id}
- PUT /api/contacts/{id}
- PATCH /api/contacts/{id}/consent — {"consent":true}
- PATCH /api/contacts/{id}/block — {"blocked":true}
- DELETE /api/contacts/{id}
- POST /api/contacts/import — {"contatos":[{"nome":"Ana","telefone":"85999998888"}]}

Exemplo:
```bash
curl -X POST localhost:8082/api/contacts \
 -H 'Content-Type: application/json' \
 -H 'X-Company-Id: 11111111-1111-4111-8111-111111111111' \
 -d '{"name":"Ana","phone":"(85) 99999-8888","marketingConsent":false}'
```

### Import Service

O importador atual produz `contatos` com `nome` e `telefone`. Encaminhe apenas os contatos validados ao endpoint de importação. Importados entram **sem** consentimento de marketing.

### Segurança — essencial antes de produção

**Não publique diretamente esta API.** O cabeçalho `X-Company-Id` é confiado por este MVP e pode ser falsificado. Exponha somente atrás de gateway com autenticação forte que sobrescreva esse cabeçalho, ou adicione validação de JWT com empresa derivada de claims verificadas. Registrar prova de consentimento (origem, data, finalidade), opt-out, trilha de auditoria e proteção de dados antes de campanhas reais.

A deduplicação por empresa está protegida por constraint de banco. Em importações concorrentes, conflito de unicidade pode abortar a transação; adicionar tratamento para esse caso antes de uso intenso.



## Autenticação por empresa

Mudança de contrato: rotas de negócio exigem JWT RS256 do auth-service. X-Company-Id
sozinho retorna 401. A empresa vem do claim company_id validado; cabeçalho opcional
diferente retorna 403. JWT inválido/vencido retorna 401; permissão insuficiente, 403.
Recursos de outra empresa continuam ausentes (404) ou fora da listagem.

Configuração local (padrões de desenvolvimento):

```bash
export API_AUTH_ISSUER=http://localhost:8085
export API_AUTH_JWKS_URI=http://localhost:8085/.well-known/jwks.json
```

Em implantação externa, configure URLs HTTPS. Chave privada fica apenas no auth-service.
Obter novas chaves depende da disponibilidade do auth; tokens duram 15 minutos, com
até 60 segundos de tolerância. Esta etapa não acrescenta revogação imediata, gestão de
usuários ou refresh tokens. Não registra nem persiste JWT nas mensagens RabbitMQ.

Rollout coordenado: atualizar auth, contact, campaign, import e disparo; configurar
credencial do worker nos dois serviços e refazer login. Tokens antigos têm escopos e
audiences antigos. Pausar envio/eligibilidade durante a atualização; não reenviar
campanhas existentes. Testes HTTP usam servidores locais, sem Meta ou envio real.

Audience obrigatória: `zalura-contact`. GET exige contact:read; POST/PUT/PATCH/DELETE exigem contact:write.


## Credencial do worker de disparos

O worker consulta GET /internal/companies/{companyId}/contacts/{contactId}/eligibility,
com credencial independente do login. A resposta contém apenas id, companyId, phone,
marketingConsent e blocked. Não dá acesso às rotas públicas, à listagem nem à escrita.

Gere uma credencial aleatória local de 32 bytes (por exemplo, `openssl rand -hex 32`),
guarde fora do Git e configure o MESMO CONTACT_WORKER_TOKEN em contact e disparo.
No contact, configure também CONTACT_WORKER_COMPANIES com UUIDs separados por vírgula:

```bash
export CONTACT_WORKER_COMPANIES=11111111-1111-1111-1111-111111111111
read -rsp 'Credencial do worker: ' CONTACT_WORKER_TOKEN
export CONTACT_WORKER_TOKEN
```

Sem credencial, a rota interna permanece fechada. Empresa fora da lista retorna 403.
Nunca exponha /internal/** no proxy público. Use HTTPS entre hosts; HTTP somente em
loopback. A credencial é estática e tem acesso a todas as empresas explicitamente
listadas: não serve como desenho final para provisionamento de muitos clientes.
Rotação exige trocar nos dois serviços e reiniciar. O worker trata 401/403 como
CONTACT_AUTH_ERROR e aguarda nova tentativa, sem enviar o contato. Não copie token
Meta ou JWT de usuário para esta variável. Não coloque segredos em prints ou no Git.
