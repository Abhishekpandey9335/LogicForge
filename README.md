# AP_Code Platform — Spring Boot Backend

Complete REST API backend for the AP_Code learning platform, built with **Spring Boot 3.2**, **PostgreSQL**, and **JWT authentication**.

---

## 🏗️ Tech Stack

| Layer        | Technology                          |
|-------------|-------------------------------------|
| Framework   | Spring Boot 3.2.5                   |
| Language    | Java 17                             |
| Database    | PostgreSQL 15+                      |
| ORM         | Spring Data JPA / Hibernate         |
| Security    | Spring Security + JWT (JJWT 0.12)  |
| Validation  | Jakarta Validation (Bean Validation)|
| Boilerplate | Lombok                              |

---

## ⚡ Quick Start

### 1. Prerequisites

```bash
# Java 17
java -version

# Maven 3.8+
mvn -version

# PostgreSQL running
psql --version
```

### 2. Create PostgreSQL Database

```bash
psql -U postgres
```
```sql
CREATE DATABASE apcode_db;
CREATE USER apcode_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE apcode_db TO apcode_user;
\q
```

### 3. Configure `application.properties`

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/apcode_db
spring.datasource.username=apcode_user
spring.datasource.password=your_password

# Generate a strong JWT secret (base64-encoded 256-bit key):
app.jwt.secret=YOUR_BASE64_SECRET_HERE

# Your frontend URLs
app.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500
```

**Generate a JWT secret:**
```bash
openssl rand -base64 32
```

### 4. Run the App

```bash
cd apcode-backend
mvn spring-boot:run
```

The app starts at **`http://localhost:8080/api`** and auto-seeds:
- Admin user: `admin@apcode.in` / `Admin@2026`
- 4 courses (java, dsa, web, interview)
- 5 sample videos

---

## 📡 API Reference

All endpoints are prefixed with `/api`.

### 🔐 Auth

| Method | Endpoint          | Auth | Description         |
|--------|-------------------|------|---------------------|
| POST   | `/auth/register`  | ❌   | Create account      |
| POST   | `/auth/login`     | ❌   | Login → JWT token   |

**Register:**
```json
POST /api/auth/register
{
  "fullName": "Rahul Sharma",
  "email": "rahul@example.com",
  "password": "secure123",
  "city": "Delhi"
}
```

**Login:**
```json
POST /api/auth/login
{
  "email": "rahul@example.com",
  "password": "secure123"
}
```
→ Returns:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGci...",
    "tokenType": "Bearer",
    "userId": 2,
    "fullName": "Rahul Sharma",
    "email": "rahul@example.com",
    "role": "STUDENT",
    "totalPoints": 0
  }
}
```

**Use token in subsequent requests:**
```
Authorization: Bearer eyJhbGci...
```

---

### 📚 Courses

| Method | Endpoint                        | Auth     | Description               |
|--------|---------------------------------|----------|---------------------------|
| GET    | `/courses`                      | Optional | All published courses      |
| GET    | `/courses/{slug}`               | Optional | Course details + progress  |
| POST   | `/courses`                      | Admin    | Create course              |
| POST   | `/courses/{slug}/enroll`        | ✅        | Enroll in course           |
| POST   | `/courses/{slug}/complete`      | ✅        | Mark video as complete     |
| GET    | `/courses/my/enrollments`       | ✅        | My enrolled courses        |

**Course slugs:** `java`, `dsa`, `web`, `interview`

**Mark video complete:**
```json
POST /api/courses/dsa/complete
{ "videoId": 3 }
```

---

### 🎬 Videos

| Method | Endpoint              | Auth  | Description          |
|--------|-----------------------|-------|----------------------|
| GET    | `/videos/free`        | ❌    | Gallery (free videos)|
| GET    | `/videos/course/{slug}` | ❌  | All videos in course |
| POST   | `/videos`             | Admin | Add video            |

---

### 👤 User Profile

| Method | Endpoint   | Auth | Description             |
|--------|------------|------|-------------------------|
| GET    | `/users/me` | ✅  | Profile + enrollments   |

---

### 📊 Leaderboard & Stats

| Method | Endpoint            | Auth | Description              |
|--------|---------------------|------|--------------------------|
| GET    | `/leaderboard`      | ❌   | Top learners by points   |
| GET    | `/leaderboard?limit=20` | ❌ | Custom limit (max 50)  |
| GET    | `/stats`            | ❌   | Platform counter stats   |

**Stats response:**
```json
{
  "data": {
    "totalStudents": 12048,
    "totalVideos": 105,
    "totalCourses": 4,
    "totalSubscribers": 5120,
    "averageRating": 4.9
  }
}
```

---

### 📬 Newsletter

| Method | Endpoint                    | Auth | Description   |
|--------|-----------------------------|------|---------------|
| POST   | `/newsletter/subscribe`     | ❌   | Subscribe     |
| POST   | `/newsletter/unsubscribe`   | ❌   | Unsubscribe   |

```json
POST /api/newsletter/subscribe
{ "email": "user@example.com" }
```

---

### ⭐ Reviews

| Method | Endpoint               | Auth  | Description         |
|--------|------------------------|-------|---------------------|
| GET    | `/reviews/public`      | ❌    | Approved reviews     |
| POST   | `/reviews`             | ✅    | Submit review        |
| PATCH  | `/reviews/{id}/approve`| Admin | Approve review       |

```json
POST /api/reviews
{
  "reviewText": "Amazing course! Cleared my basics in 2 days.",
  "rating": 5,
  "courseId": 1
}
```

---

## 🔗 Connecting to the Frontend

Add this JavaScript to your `index.html` to replace the static data with live API calls:

```javascript
const API = 'http://localhost:8080/api';

// ── Login ──────────────────────────────────────────────────
async function apiLogin(email, password) {
  const res = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  const json = await res.json();
  if (json.success) {
    localStorage.setItem('token', json.data.token);
    localStorage.setItem('user', JSON.stringify(json.data));
  }
  return json;
}

// ── Helper: authenticated fetch ────────────────────────────
function authFetch(url, options = {}) {
  const token = localStorage.getItem('token');
  return fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...options.headers
    }
  });
}

// ── Load platform stats (hero counters) ───────────────────
async function loadStats() {
  const res = await fetch(`${API}/stats`);
  const { data } = await res.json();
  document.querySelector('[data-target="12000"]').dataset.target = data.totalStudents;
  // ... update other counters
}

// ── Load free videos (gallery) ────────────────────────────
async function loadFreeVideos() {
  const res = await fetch(`${API}/videos/free`);
  const { data } = await res.json();
  // Map to videoData array used by renderGallery()
  return data.map(v => ({
    id: v.youtubeId,
    title: v.title,
    desc: v.description,
    thumbnail: v.thumbnailUrl
  }));
}

// ── Load leaderboard ──────────────────────────────────────
async function loadLeaderboard() {
  const res = await fetch(`${API}/leaderboard?limit=7`);
  const { data } = await res.json();
  // Render leaderboard rows from data
}

// ── Enroll in a course ────────────────────────────────────
async function enrollCourse(slug) {
  const res = await authFetch(`${API}/courses/${slug}/enroll`, { method: 'POST' });
  const json = await res.json();
  showToast(json.message);
}

// ── Newsletter subscribe ───────────────────────────────────
async function handleNewsletter() {
  const email = document.getElementById('nlEmail').value;
  const res = await fetch(`${API}/newsletter/subscribe`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });
  const json = await res.json();
  showToast(json.success ? '🎉 ' + json.message : '⚠️ ' + json.message);
}
```

---

## 🏗️ Project Structure

```
apcode-backend/
├── src/main/java/com/apcode/
│   ├── ApcodeApplication.java        # Entry point
│   ├── config/
│   │   ├── SecurityConfig.java       # JWT + CORS + route auth
│   │   └── DataSeeder.java           # Auto-seeds courses/videos
│   ├── controller/
│   │   ├── AuthController.java       # /auth/*
│   │   └── Controllers.java          # All other controllers
│   ├── dto/                          # Request/Response objects
│   ├── entity/                       # JPA entities (DB tables)
│   ├── exception/                    # Error handling
│   ├── repository/                   # Spring Data JPA repos
│   ├── security/                     # JWT filter + UserDetails
│   └── service/                      # Business logic
├── src/main/resources/
│   ├── application.properties        # App config
│   └── schema.sql                    # Reference SQL schema
└── pom.xml                           # Dependencies
```

---

## 🔐 Gamification Points System

| Action              | Points Awarded |
|---------------------|---------------|
| Register account    | —             |
| Enroll in a course  | +50 pts       |
| Complete a lecture  | +10 pts       |

Points are used for leaderboard ranking.

---

## 🚀 Production Checklist

- [ ] Change `app.jwt.secret` to a strong random 256-bit key
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (never `update` in prod)
- [ ] Use environment variables for DB credentials (not hardcoded)
- [ ] Configure a reverse proxy (Nginx) in front of the app
- [ ] Set `logging.level.root=WARN` in production
- [ ] Enable HTTPS / SSL certificates (Let's Encrypt)
- [ ] Restrict `app.cors.allowed-origins` to your production domain only
