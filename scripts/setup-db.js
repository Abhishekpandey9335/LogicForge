// scripts/setup-db.js
// Run once: node scripts/setup-db.js
// Creates the "apcode" PostgreSQL database and all required tables.

const { Client } = require("pg");
require("dotenv").config({ path: require("path").join(__dirname, "../.env") });

async function setupDatabase() {
  // Step 1 — connect to default "postgres" DB to create "apcode" DB if needed
  const adminClient = new Client({
    host:     process.env.DB_HOST     || "localhost",
    port:     parseInt(process.env.DB_PORT) || 5432,
    database: "postgres",
    user:     process.env.DB_USER     || "postgres",
    password: process.env.DB_PASSWORD || "",
  });

  try {
    await adminClient.connect();
    console.log("🔗 Connected to PostgreSQL as admin...");

    // Create the apcode database if it doesn't exist
    const dbCheck = await adminClient.query(
      `SELECT 1 FROM pg_database WHERE datname = 'apcode'`
    );

    if (dbCheck.rowCount === 0) {
      await adminClient.query(`CREATE DATABASE apcode`);
      console.log("✅ Database 'apcode' created successfully.");
    } else {
      console.log("ℹ️  Database 'apcode' already exists.");
    }

    await adminClient.end();

    // Step 2 — connect to the apcode database and create tables
    const appClient = new Client({
      host:     process.env.DB_HOST     || "localhost",
      port:     parseInt(process.env.DB_PORT) || 5432,
      database: "apcode",
      user:     process.env.DB_USER     || "postgres",
      password: process.env.DB_PASSWORD || "",
    });

    await appClient.connect();
    console.log("🔗 Connected to 'apcode' database...");

    // ── USERS TABLE ───────────────────────────────────────
    await appClient.query(`
      CREATE TABLE IF NOT EXISTS users (
        id            SERIAL PRIMARY KEY,
        name          VARCHAR(100),
        email         VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        role          VARCHAR(20)  DEFAULT 'student',
        is_active     BOOLEAN      DEFAULT true,
        join_date     TIMESTAMPTZ  DEFAULT NOW(),
        last_login    TIMESTAMPTZ,
        login_count   INTEGER      DEFAULT 0,
        city          VARCHAR(100),
        phone         VARCHAR(20),
        avatar_url    TEXT
      );
    `);
    console.log("✅ Table: users");

    // ── SESSIONS TABLE ────────────────────────────────────
    await appClient.query(`
      CREATE TABLE IF NOT EXISTS sessions (
        id          SERIAL PRIMARY KEY,
        user_id     INTEGER REFERENCES users(id) ON DELETE CASCADE,
        token       TEXT NOT NULL,
        ip_address  VARCHAR(50),
        user_agent  TEXT,
        created_at  TIMESTAMPTZ DEFAULT NOW(),
        expires_at  TIMESTAMPTZ,
        is_active   BOOLEAN     DEFAULT true
      );
    `);
    console.log("✅ Table: sessions");

    // ── NEWSLETTER SUBSCRIBERS TABLE ──────────────────────
    await appClient.query(`
      CREATE TABLE IF NOT EXISTS newsletter_subscribers (
        id           SERIAL PRIMARY KEY,
        email        VARCHAR(255) UNIQUE NOT NULL,
        subscribed_at TIMESTAMPTZ DEFAULT NOW(),
        is_active    BOOLEAN     DEFAULT true
      );
    `);
    console.log("✅ Table: newsletter_subscribers");

    // ── COURSE ENROLLMENTS TABLE ──────────────────────────
    await appClient.query(`
      CREATE TABLE IF NOT EXISTS enrollments (
        id           SERIAL PRIMARY KEY,
        user_id      INTEGER REFERENCES users(id) ON DELETE CASCADE,
        course_slug  VARCHAR(50) NOT NULL,
        enrolled_at  TIMESTAMPTZ DEFAULT NOW(),
        progress     INTEGER     DEFAULT 0,
        completed    BOOLEAN     DEFAULT false,
        UNIQUE(user_id, course_slug)
      );
    `);
    console.log("✅ Table: enrollments");

    // ── CONTACT MESSAGES TABLE ────────────────────────────
    await appClient.query(`
      CREATE TABLE IF NOT EXISTS contact_messages (
        id         SERIAL PRIMARY KEY,
        name       VARCHAR(100),
        email      VARCHAR(255),
        message    TEXT,
        is_read    BOOLEAN     DEFAULT false,
        created_at TIMESTAMPTZ DEFAULT NOW()
      );
    `);
    console.log("✅ Table: contact_messages");

    // ── ANNOUNCEMENTS TABLE ───────────────────────────────
    await appClient.query(`
      CREATE TABLE IF NOT EXISTS announcements (
        id         SERIAL PRIMARY KEY,
        message    TEXT NOT NULL,
        link_text  VARCHAR(100),
        link_url   VARCHAR(500),
        is_active  BOOLEAN     DEFAULT true,
        created_at TIMESTAMPTZ DEFAULT NOW()
      );
    `);
    console.log("✅ Table: announcements");

    // ── ADMIN ACTIONS LOG TABLE ───────────────────────────
    await appClient.query(`
      CREATE TABLE IF NOT EXISTS admin_logs (
        id          SERIAL PRIMARY KEY,
        action      VARCHAR(200) NOT NULL,
        details     TEXT,
        performed_at TIMESTAMPTZ DEFAULT NOW()
      );
    `);
    console.log("✅ Table: admin_logs");

    // ── INDEXES ───────────────────────────────────────────
    await appClient.query(`CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);`);
    await appClient.query(`CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id);`);
    await appClient.query(`CREATE INDEX IF NOT EXISTS idx_enrollments_user ON enrollments(user_id);`);
    await appClient.query(`CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token);`);
    console.log("✅ Indexes created");

    await appClient.end();

    console.log("\n🎉 Database setup complete! All tables created in 'apcode'.");
    console.log("   Run: npm start  — to start the server\n");
  } catch (err) {
    console.error("❌ Setup failed:", err.message);
    process.exit(1);
  }
}

setupDatabase();
