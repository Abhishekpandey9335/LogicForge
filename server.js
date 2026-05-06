// server.js — AP_Code Backend Entry Point
// PostgreSQL database: apcode
require("dotenv").config();

const express = require("express");
const cors    = require("cors");
const app     = express();

// ── MIDDLEWARE ────────────────────────────────────────────
app.use(cors({
  origin: [
    "http://localhost:3000",
    "http://localhost:5500",
    "http://127.0.0.1:5500",
    "http://localhost:8080",
    // Add your production domain here:
    // "https://yourdomain.com"
  ],
  credentials: true,
  methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization", "x-admin-password"],
}));

app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true }));

// ── REQUEST LOGGER (development) ─────────────────────────
if (process.env.NODE_ENV !== "production") {
  app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
    next();
  });
}

// ── HEALTH CHECK ─────────────────────────────────────────
app.get("/", (req, res) => {
  res.json({
    success: true,
    message: "AP_Code API is running 🚀",
    version: "1.0.0",
    database: "apcode (PostgreSQL)",
    timestamp: new Date().toISOString(),
    endpoints: {
      auth:       "/api/signup, /api/login, /api/logout, /api/me",
      courses:    "/api/courses/enroll, /api/courses/progress, /api/courses/my",
      newsletter: "/api/newsletter/subscribe, /api/newsletter/unsubscribe",
      contact:    "/api/contact",
      admin:      "/api/admin/dashboard (requires x-admin-password header)",
    },
  });
});

// ── ROUTES ────────────────────────────────────────────────
const authRoutes       = require("./routes/auth");
const adminRoutes      = require("./routes/admin");
const newsletterRoutes = require("./routes/newsletter");
const courseRoutes     = require("./routes/courses");

app.use("/api",           authRoutes);
app.use("/api/admin",     adminRoutes);
app.use("/api/newsletter",newsletterRoutes);
app.use("/api/contact",   newsletterRoutes);  // contact POST is in newsletter.js
app.use("/api/courses",   courseRoutes);

// ── 404 HANDLER ───────────────────────────────────────────
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Route not found: ${req.method} ${req.path}`,
  });
});

// ── GLOBAL ERROR HANDLER ──────────────────────────────────
app.use((err, req, res, next) => {
  console.error("Unhandled error:", err.stack);
  res.status(500).json({
    success: false,
    message: "Internal server error.",
    ...(process.env.NODE_ENV !== "production" && { error: err.message }),
  });
});

// ── START SERVER ──────────────────────────────────────────
const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
  console.log("\n╔════════════════════════════════════════╗");
  console.log("║      AP_Code Backend Server            ║");
  console.log("╠════════════════════════════════════════╣");
  console.log(`║  🚀 Server running on port ${PORT}        ║`);
  console.log(`║  🗄️  Database: apcode (PostgreSQL)      ║`);
  console.log(`║  🌍 Mode: ${process.env.NODE_ENV || "development"}                  ║`);
  console.log("╚════════════════════════════════════════╝\n");
});

module.exports = app;
