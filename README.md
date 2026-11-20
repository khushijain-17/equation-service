# Equation Service

Spring Boot (Java 21) REST service that **stores algebraic equations as postfix expression trees**, lists them back in
infix form, and **evaluates** them with supplied variable values. Storage is in-memory (as the assignment specifies).

- **Live URL:** `https://equation-service.onrender.com`
- **Swagger UI:** `https://equation-service.onrender.com/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`
- **Health:** `/actuator/health`

## How it works

```
"3x + 2y - z" ──Tokenizer──▶ tokens ──PostfixConverter (shunting-yard)──▶ 3 x * 2 y * + z -
                                                       └──ExpressionTreeBuilder (operand stack)──▶ tree
              tree ──InfixFormatter (in-order)──▶ "3x + 2y - z"     tree ──ExpressionEvaluator (post-order)──▶ 11
```

| Class | Responsibility |
|---|---|
| `parser.Tokenizer` | text → tokens, rejects unknown characters / malformed numbers |
| `parser.PostfixConverter` | shunting-yard; validates syntax, handles unary minus, implicit multiplication |
| `parser.ExpressionTreeBuilder` | postfix → tree (operators are parents, operands are children) |
| `tree.ExpressionNode` | sealed interface: `NumberNode`, `VariableNode`, `UnaryMinusNode`, `BinaryNode` |
| `tree.ExpressionEvaluator` / `InfixFormatter` | tree traversals (exhaustive `switch` over the sealed type) |
| `repository.EquationRepository` | storage interface; `InMemoryEquationRepository` is thread-safe |
| `service.EquationService` / `controller.EquationController` | orchestration / REST |
| `exception.GlobalExceptionHandler` | consistent JSON errors |

### Supported syntax
`+ - * / ^`, parentheses, decimals (`1.5`), unary minus/plus, single-letter variables (case-sensitive), implicit
multiplication (`3x`, `2(x+1)`, `(a)(b)`, `xy` = x·y). `^` is right-associative; `-x^2` = `-(x^2)`.
Equations with `=` are rejected (store the expression, e.g. `x^2 - 4`).

**Real-world input is normalised before parsing** (`parser.InputNormalizer`): `× · ÷ − – —` become `* * / - - -`,
`²`/`³` become `^2`/`^3`, Python-style `x**2` becomes `x^2`, and non-breaking/thin/zero-width spaces become spaces.
So `x² + y² − 4` pasted from Word or a phone works.

**Precision:** results are rounded to 15 significant digits (a `double` holds ~15.95), so `0.1 + 0.2` returns `0.3`
and `4.35 * 100` returns `435`, instead of binary floating-point noise.

## API

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/equations/store` | 201 `{message, equationId, postfix}` + `Location` header | 400 |
| GET | `/api/equations` | 200 `{equations:[{equationId, equation}]}` | |
| GET | `/api/equations/{id}` | 200 `{equationId, equation}` | 404 |
| DELETE | `/api/equations/{id}` | 204 (ids are never reused) | 404 |
| POST | `/api/equations/{id}/evaluate` | 200 `{equationId, equation, variables, result}` | 400, 404, 422 |

```bash
curl -X POST localhost:8080/api/equations/store -H 'Content-Type: application/json' -d '{"equation":"3x + 2y - z"}'
# {"message":"Equation stored successfully","equationId":"1","postfix":"3 x * 2 y * + z -"}

curl localhost:8080/api/equations
# {"equations":[{"equationId":"1","equation":"3x + 2y - z"}]}

curl -X POST localhost:8080/api/equations/1/evaluate -H 'Content-Type: application/json' \
     -d '{"variables":{"x":2,"y":3,"z":1}}'
# {"equationId":"1","equation":"3x + 2y - z","variables":{"x":2,"y":3,"z":1},"result":11}
```

> **Note on the assignment's example:** the PDF shows `result: 10` for `3x + 2y - z` at x=2, y=3, z=1, but
> 3·2 + 2·3 − 1 = **11**, which is what this service returns.

### Errors
All errors share one shape: `{timestamp, status, error, message, details[]}`.

| Status | When |
|---|---|
| 400 | bad syntax (message names the position), blank/missing `equation`, malformed JSON, non-numeric variable value |
| 404 | unknown equation id (also when deleting) |
| 409 | storage limit reached (default 1000 equations, set with `equations.max-stored`); delete some to free space |
| 422 | missing variables (`details` lists them), division by zero, undefined/overflowing result |

## Run

Requires JDK 21 and Maven 3.9+.

```bash
mvn spring-boot:run          # http://localhost:8080/swagger-ui.html
mvn test                     # unit + integration tests
docker build -t equation-service . && docker run -p 8080:8080 equation-service
```

Import `postman_collection.json` into Postman (set `baseUrl`) for ready-made requests including error cases.

## Tests

`mvn test` runs:
- `EquationParserTest` – postfix conversion, infix reconstruction, round-trip, ~17 invalid inputs, error messages,
  pasted symbols (`× ÷ − ² **`) and unicode spaces
- `ExpressionEvaluatorTest` – precedence/associativity, unary minus, variables, missing vars, div-by-zero, overflow,
  floating-point noise (`0.1 + 0.2 = 0.3`)
- `EquationServiceTest` – ids, ordering, not-found, delete (ids not reused), storage limit, concurrent stores
- `EquationControllerTest` – every endpoint over MockMvc incl. all error statuses and the OpenAPI spec
- `StorageLimitControllerTest` – full-stack 409 when the limit is reached, freed again by DELETE

## Deploy (Render, free tier)

1. Push this folder to a public GitHub repo.
2. Render → **New + → Blueprint** (or *Web Service* → Runtime *Docker*) → select the repo. `render.yaml` is included.
3. Once live, open `<url>/swagger-ui.html`. (Free instances sleep after inactivity; the first request may take ~30–60 s —
   hit `/actuator/health` shortly before you submit/record.)

Storage is in-memory, so data resets on restart/redeploy; swapping in a database only needs a new `EquationRepository`.
