// config/db.js — PostgreSQL connection pool
const { Pool } = require("pg");
require("dotenv").config();

const pool = new Pool({
  host:     process.env.DB_HOST     || "localhost",
  port:     parseInt(process.env.DB_PORT) || 5432,
  database: process.env.DB_NAME     || "apcode",
  user:     process.env.DB_USER     || "postgres",
password: typeof process.env.DB_PASSWORD === "string" && process.env.DB_PASSWORD.length > 0
  ? process.env.DB_PASSWORD
  : "1122",
  // Connection pool settings
  max: 20,                  // max pool size
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,    
});

// Test connection on startup
pool.connect((err, client, release) => {
  if (err) {
    console.error("❌ PostgreSQL connection failed:", err.message);
    console.error("   Make sure PostgreSQL is running and .env is configured correctly.");
  } else {
    release();
    console.log("✅ PostgreSQL connected — database: apcode");
  }
});

module.exports = pool;
