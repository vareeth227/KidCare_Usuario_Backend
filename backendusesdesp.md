# Despliegue — Usuario Backend (puerto 8081)

Registro de los pasos ejecutados para levantar el servicio desde cero.

---

## Lo que se hizo

### 1. Diagnóstico inicial
Se intentó correr el servicio directamente con `mvn spring-boot:run`. Falló con:
```
Communications link failure — The driver has not received any packets from the server.
```
Causa: Docker Desktop no estaba corriendo, por lo que MySQL no existía.

### 2. Inicio de Docker Desktop
Se inició Docker Desktop manualmente desde el menú de inicio y se esperó a que el daemon estuviera activo (`docker ps` sin error).

### 3. Creación del `docker-compose.yml`
El repositorio no incluye un archivo de compose. Se creó en `KidCare_Usuario_Backend/docker-compose.yml` con el siguiente contenido:

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: kidcare-mysql
    environment:
      MYSQL_ROOT_PASSWORD: kidcare123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    command: >
      --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci

volumes:
  mysql_data:
```

### 4. Levantamiento del contenedor MySQL
```bash
docker compose up -d
```
Docker descargó la imagen `mysql:8.0` y creó el contenedor `kidcare-mysql` con el volumen `mysql_data` para persistencia.

### 5. Creación de la base de datos
Se esperaron ~20 segundos para que MySQL terminara de inicializar, luego:
```bash
docker exec kidcare-mysql mysql -u root -pkidcare123 \
  -e "CREATE DATABASE IF NOT EXISTS db_usuario CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```
Se verificó con `SHOW DATABASES;` que `db_usuario` apareció en la lista.

### 6. Inicio del servicio Spring Boot
```bash
mvn spring-boot:run
```
Resultado exitoso:
```
Tomcat started on port 8081 (http)
Started UsuarioServiceApplication in 3.649 seconds
```
Hibernate creó automáticamente las tablas vía `ddl-auto=update` y el `data.sql` sembró los roles y permisos iniciales.

---

## Estado final

| Componente | Estado | Detalle |
|---|---|---|
| Docker Desktop | Corriendo | — |
| Contenedor MySQL | Up | `kidcare-mysql`, puerto 3306 |
| Base de datos | Creada | `db_usuario` |
| Usuario Backend | Corriendo | `http://localhost:8081` |

---

## Tests de la API

Los tres tests cubren el flujo principal: registro → login → operación autenticada.

> **Nota:** Los tests están escritos en PowerShell (Windows). Para Mac/Linux reemplaza `Invoke-RestMethod` por `curl` (ejemplos incluidos).

---

### Test 1 — Registro de usuario

Crea una cuenta TUTOR nueva. Debe devolver un JSON con `token`, `email` y `rol`.

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/auth/registro" `
  -Method POST `
  -ContentType "application/json" `
  -Body 'u'
```

**curl (Mac/Linux):**
```bash
curl -s -X POST http://localhost:8081/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{
    "nombreCompleto": "Ana García",
    "email": "ana.garcia@test.com",
    "password": "Password123",
    "rolNombre": "TUTOR",
    "aceptaTerminos": true
  }' | jq
```

**Respuesta esperada (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "ana.garcia@test.com",
  "rol": "TUTOR"
}
```

**Falla si:** el email ya existe → 400 `"El correo ya está registrado"`.

---

### Test 2 — Login

Autentica con las credenciales del usuario registrado en el Test 1. Guarda el `token` devuelto, lo necesitas en el Test 3.

**PowerShell:**
```powershell
$respuesta = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{
    "email": "ana.garcia@test.com",
    "password": "Password123"
  }'

$token = $respuesta.token
Write-Host "Token: $token"
```

**curl (Mac/Linux):**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.garcia@test.com","password":"Password123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"
```

**Respuesta esperada (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "ana.garcia@test.com",
  "rol": "TUTOR"
}
```

**Falla si:** contraseña incorrecta → 400 `"Credenciales inválidas"`.

---

### Test 3 — Crear perfil de menor (endpoint protegido)

Crea un menor vinculado al TUTOR autenticado. Requiere el JWT del Test 2 en el header `Authorization`.

**PowerShell:**
```powershell
# Usa $token guardado en el Test 2
Invoke-RestMethod -Uri "http://localhost:8081/api/menores" `
  -Method POST `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body '{
    "nombreCompleto": "Carlos García",
    "fechaNacimiento": "2018-04-15",
    "sexo": "M"
  }'
```

**curl (Mac/Linux):**
```bash
curl -s -X POST http://localhost:8081/api/menores \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombreCompleto": "Carlos García",
    "fechaNacimiento": "2018-04-15",
    "sexo": "M"
  }' | jq
```

**Respuesta esperada (200):**
```json
{
  "id": 1,
  "nombreCompleto": "Carlos García",
  "fechaNacimiento": "2018-04-15",
  "sexo": "M"
}
```

**Falla si:** no se envía el header `Authorization` → 403 Forbidden.

---

## Comandos útiles de mantenimiento

```bash
# Ver logs del contenedor MySQL
docker logs kidcare-mysql

# Detener el contenedor sin borrar datos
docker compose stop

# Reiniciar el contenedor
docker compose start

# Eliminar contenedor Y datos (destruye la base de datos)
docker compose down -v

# Verificar que el puerto 8081 está ocupado
netstat -ano | findstr :8081
```
