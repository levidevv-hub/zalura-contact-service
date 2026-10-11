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
