# Graph Report - wallet-api  (2026-10-27)

## Corpus Check
- 56 files · ~4,942 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 4 file(s) not represented in the graph (top: (none) 2, .properties 1, .cmd 1)

## Summary
- 361 nodes · 781 edges · 16 communities (8 shown, 8 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 54 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `936d32d6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- AccountService
- BusinessException
- SecurityConfig.java
- CurrencyCode
- AuthService
- AccountEntryRepository.java
- spring
- AccountController.java
- postgres
- .isValid
- GlobalExceptionHandler.java
- WalletApiApplication.java
- dev.umut:wallet-api

## God Nodes (most connected - your core abstractions)
1. `BusinessException` - 29 edges
2. `AccountService` - 28 edges
3. `CurrencyCode` - 21 edges
4. `AccountEntryResponse` - 15 edges
5. `Account` - 14 edges
6. `AccountEntry` - 13 edges
7. `AuthService` - 12 edges
8. `TransferService` - 12 edges
9. `AccountEntryPaginationIT` - 12 edges
10. `AccountController` - 11 edges

## Surprising Connections (you probably didn't know these)
- `AccountRepository` --references--> `Account`  [EXTRACTED]
  src/main/java/dev/umut/wallet_api/account/AccountRepository.java → src/main/java/dev/umut/wallet_api/account/Account.java
- `AccountController` --references--> `AccountService`  [EXTRACTED]
  src/main/java/dev/umut/wallet_api/account/AccountController.java → src/main/java/dev/umut/wallet_api/account/AccountService.java
- `AccountEntryRepository` --references--> `AccountEntry`  [EXTRACTED]
  src/main/java/dev/umut/wallet_api/account/AccountEntryRepository.java → src/main/java/dev/umut/wallet_api/account/AccountEntry.java
- `AccountService` --references--> `AccountEntryRepository`  [EXTRACTED]
  src/main/java/dev/umut/wallet_api/account/AccountService.java → src/main/java/dev/umut/wallet_api/account/AccountEntryRepository.java
- `AccountService` --references--> `AccountNumberGenerator`  [EXTRACTED]
  src/main/java/dev/umut/wallet_api/account/AccountService.java → src/main/java/dev/umut/wallet_api/account/AccountNumberGenerator.java

## Import Cycles
- None detected.

## Communities (16 total, 8 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.09
Nodes (7): AccountResponse, CursorPage, OpenAccountRequest, AccountEntryPaginationIT, ConcurrentWithdrawalIT, ConcurrentTransferIT, WalletApiApplicationTests

### Community 1 - "AccountService"
Cohesion: 0.11
Nodes (10): AccountEntry, AccountEntryResponse, AccountService, EntryType, DEPOSIT, TRANSFER_IN, TRANSFER_OUT, WITHDRAWAL (+2 more)

### Community 2 - "BusinessException"
Cohesion: 0.09
Nodes (13): AccountAlreadyExistsException, AccountNotActiveException, AccountNotFoundException, InsufficientFundsException, InvalidCursorException, EmailAlreadyRegisteredException, InvalidCredentialsException, BusinessException (+5 more)

### Community 4 - "CurrencyCode"
Cohesion: 0.09
Nodes (13): Account, AccountStatus, ACTIVE, CLOSED, FROZEN, CurrencyCode, EUR, TRY (+5 more)

### Community 5 - "AuthService"
Cohesion: 0.11
Nodes (8): AuthController, AuthService, LoginRequest, RegisterRequest, TokenResponse, User, UserRepository, UserResponse

### Community 6 - "AccountEntryRepository.java"
Cohesion: 0.13
Nodes (3): AccountEntryRepository, AccountNumberGenerator, AccountRepository

### Community 7 - "spring"
Cohesion: 0.09
Nodes (22): app, jwt, name, web, password, url, username, ddl-auto (+14 more)

### Community 10 - "postgres"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

## Knowledge Gaps
- **11 isolated node(s):** `dev.umut:wallet-api`, `ACTIVE`, `FROZEN`, `CLOSED`, `TRY` (+6 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 92 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AccountService` connect `AccountService` to `org.junit.jupiter.api.Test`, `AccountController.java`, `CurrencyCode`, `AccountEntryRepository.java`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **What connects `dev.umut:wallet-api`, `ACTIVE`, `FROZEN` to the rest of the system?**
  _11 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.08525506638714186 - nodes in this community are weakly interconnected._
- **Why does `BusinessException` connect `BusinessException` to `uuid`, `GlobalExceptionHandler.java`?**
  _High betweenness centrality (0.088) - this node is a cross-community bridge._
- **Should `AccountService` be split into smaller, more focused modules?**
  _Cohesion score 0.1066066066066066 - nodes in this community are weakly interconnected._
- **Why does `CurrencyCode` connect `CurrencyCode` to `org.junit.jupiter.api.Test`, `AccountService`, `BusinessException`, `AccountEntryRepository.java`, `uuid`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Should `BusinessException` be split into smaller, more focused modules?**
  _Cohesion score 0.09309309309309309 - nodes in this community are weakly interconnected._