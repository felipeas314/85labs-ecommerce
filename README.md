# 85Labs E-commerce API

API REST reativa para e-commerce construída com **Vert.x**, **PostgreSQL** e **JWT Authentication**.

## Stack Tecnológica

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| **Java** | 21 | Linguagem principal |
| **Vert.x** | 4.5.1 | Framework reativo |
| **PostgreSQL** | 16 | Banco de dados |
| **Flyway** | 10.4.1 | Migrations do banco |
| **JWT** | - | Autenticação |
| **BCrypt** | - | Hash de senhas |
| **Jackson** | 2.16.0 | Serialização JSON |
| **SLF4J + Logback** | 1.4.14 | Logging |

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      HttpServerVerticle                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │  Auth   │  │ Product │  │Category │  │  Order  │  Routers   │
│  │ Router  │  │ Router  │  │ Router  │  │ Router  │            │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘            │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Handlers                               │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │  Auth   │  │ Product │  │Category │  │  Order  │            │
│  │ Handler │  │ Handler │  │ Handler │  │ Handler │            │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘            │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Services                               │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │  Auth   │  │ Product │  │Category │  │  Order  │            │
│  │ Service │  │ Service │  │ Service │  │ Service │            │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘            │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Repositories                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │  User   │  │ Product │  │Category │  │  Order  │            │
│  │  Repo   │  │  Repo   │  │  Repo   │  │  Repo   │            │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘            │
└───────┴────────────┴────────────┴────────────┴──────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PostgreSQL (Reactive)                        │
└─────────────────────────────────────────────────────────────────┘
```

## Estrutura do Projeto

```
src/main/java/br/com/labs/
├── Main.java                    # Entry point
├── MainVerticle.java            # Inicialização e Flyway
├── config/
│   └── AppConfig.java           # Configurações
├── verticle/
│   └── HttpServerVerticle.java  # Servidor HTTP
├── router/
│   ├── AuthRouter.java
│   ├── ProductRouter.java
│   ├── CategoryRouter.java
│   └── OrderRouter.java
├── handler/
│   ├── AuthHandler.java
│   ├── ProductHandler.java
│   ├── CategoryHandler.java
│   ├── OrderHandler.java
│   └── ErrorHandler.java
├── service/
│   ├── AuthService.java
│   ├── ProductService.java
│   ├── CategoryService.java
│   ├── OrderService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── ProductServiceImpl.java
│       ├── CategoryServiceImpl.java
│       └── OrderServiceImpl.java
├── repository/
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   ├── OrderRepository.java
│   └── impl/
│       ├── UserRepositoryPg.java
│       ├── ProductRepositoryPg.java
│       ├── CategoryRepositoryPg.java
│       └── OrderRepositoryPg.java
├── model/
│   ├── User.java
│   ├── Product.java
│   ├── Category.java
│   ├── Order.java
│   └── OrderItem.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── CreateProductRequest.java
│   │   ├── UpdateProductRequest.java
│   │   ├── CreateCategoryRequest.java
│   │   └── CreateOrderRequest.java
│   └── response/
│       ├── ApiResponse.java
│       ├── TokenResponse.java
│       └── PageResponse.java
├── security/
│   ├── JwtProvider.java
│   └── PasswordEncoder.java
└── exception/
    ├── NotFoundException.java
    ├── ValidationException.java
    └── UnauthorizedException.java
```

## Requisitos

- Java 21+
- Maven 3.8+
- Docker e Docker Compose (para PostgreSQL)

## Instalação e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/85labs-ecommerce.git
cd 85labs-ecommerce
```

### 2. Inicie o PostgreSQL

```bash
docker-compose up -d
```

Isso irá criar um container PostgreSQL com:
- **Host**: localhost
- **Porta**: 5432
- **Database**: ecommerce
- **Usuário**: ecommerce
- **Senha**: ecommerce123

### 3. Execute a aplicação

```bash
# Com Maven
mvn compile exec:java

# Ou gere o JAR e execute
mvn clean package
java -jar target/ecommerce-1.0-SNAPSHOT.jar
```

A API estará disponível em `http://localhost:8080`

### 4. Acesse a documentação

- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/openapi.yaml
- **Health Check**: http://localhost:8080/health

## API Endpoints

### Autenticação (público)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/v1/auth/register` | Registrar novo usuário |
| `POST` | `/api/v1/auth/login` | Login (retorna JWT) |

### Produtos (requer JWT)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/products` | Listar produtos (paginado) |
| `GET` | `/api/v1/products/:id` | Buscar produto por ID |
| `POST` | `/api/v1/products` | Criar produto |
| `PUT` | `/api/v1/products/:id` | Atualizar produto |
| `DELETE` | `/api/v1/products/:id` | Deletar produto |

### Categorias (requer JWT)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/categories` | Listar categorias |
| `GET` | `/api/v1/categories/:id` | Buscar categoria por ID |
| `POST` | `/api/v1/categories` | Criar categoria |

### Pedidos (requer JWT)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/orders` | Listar pedidos do usuário |
| `GET` | `/api/v1/orders/:id` | Buscar pedido por ID |
| `POST` | `/api/v1/orders` | Criar pedido |

## Exemplos de Uso

### Registrar usuário

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com",
    "password": "senha123"
  }'
```

**Resposta:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "expiresIn": 3600
  }
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@email.com",
    "password": "senha123"
  }'
```

### Criar categoria (autenticado)

```bash
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <seu-token>" \
  -d '{
    "name": "Eletrônicos",
    "description": "Produtos eletrônicos em geral"
  }'
```

### Criar produto (autenticado)

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <seu-token>" \
  -d '{
    "name": "Smartphone XYZ",
    "description": "Smartphone de última geração",
    "code": "SMART-001",
    "price": 2499.99,
    "categoryId": "<category-uuid>"
  }'
```

### Listar produtos (autenticado)

```bash
curl http://localhost:8080/api/v1/products?page=0&size=10 \
  -H "Authorization: Bearer <seu-token>"
```

**Resposta:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "Smartphone XYZ",
        "description": "Smartphone de última geração",
        "code": "SMART-001",
        "price": 2499.99,
        "categoryId": "uuid",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### Criar pedido (autenticado)

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <seu-token>" \
  -d '{
    "items": [
      {
        "productId": "<product-uuid>",
        "quantity": 2
      },
      {
        "productId": "<product-uuid-2>",
        "quantity": 1
      }
    ]
  }'
```

## Banco de Dados

### Schema

```sql
-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Products
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    code VARCHAR(50) UNIQUE NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    category_id UUID REFERENCES categories(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Orders
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    total DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Order Items
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE NOT NULL,
    product_id UUID REFERENCES products(id) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL
);
```

### Status de Pedido

| Status | Descrição |
|--------|-----------|
| `PENDING` | Pedido criado, aguardando processamento |
| `CONFIRMED` | Pedido confirmado |
| `PROCESSING` | Em processamento |
| `SHIPPED` | Enviado |
| `DELIVERED` | Entregue |
| `CANCELLED` | Cancelado |

## Configuração

O arquivo `src/main/resources/application.json` contém as configurações:

```json
{
  "server": {
    "port": 8080,
    "host": "0.0.0.0"
  },
  "database": {
    "host": "localhost",
    "port": 5432,
    "database": "ecommerce",
    "user": "ecommerce",
    "password": "ecommerce123",
    "maxPoolSize": 10
  },
  "jwt": {
    "secret": "your-super-secret-key-change-in-production-min-256-bits",
    "issuer": "85labs-ecommerce",
    "expirationMinutes": 60
  }
}
```

### Variáveis de Ambiente (Produção)

Para produção, configure via variáveis de ambiente:

```bash
export DB_HOST=seu-host
export DB_PORT=5432
export DB_NAME=ecommerce
export DB_USER=seu-usuario
export DB_PASSWORD=sua-senha-segura
export JWT_SECRET=sua-chave-secreta-de-256-bits
```

## Respostas da API

### Sucesso

```json
{
  "success": true,
  "data": { ... }
}
```

### Erro

```json
{
  "success": false,
  "error": "Mensagem de erro"
}
```

### Códigos HTTP

| Código | Descrição |
|--------|-----------|
| `200` | OK |
| `201` | Created |
| `204` | No Content (delete) |
| `400` | Bad Request (validação) |
| `401` | Unauthorized (JWT inválido/ausente) |
| `404` | Not Found |
| `500` | Internal Server Error |

## Desenvolvimento

### Compilar

```bash
mvn clean compile
```

### Executar testes

```bash
mvn test
```

### Gerar JAR

```bash
mvn clean package
```

### Parar PostgreSQL

```bash
docker-compose down
```

### Limpar dados do banco

```bash
docker-compose down -v
```

## Licença

MIT License
