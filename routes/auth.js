// routes/auth.js — Signup, Login, Logout, Profile
const express = require("express");
const router  = express.Router();
const bcrypt  = require("bcryptjs");
const jwt     = require("jsonwebtoken");
const pool    = require("../config/db");
const { verifyToken } = require("../middleware/auth");

// ── POST /api/signup ──────────────────────────────────────
router.post("/signup", async (req, res) => {
  try {
    const { name, email, password, city, phone } = req.body;

    // Validation
    if (!email || !password) {
      return res.status(400).json({ success: false, message: "Email and password are required." });
    }
    if (password.length < 6) {
      return res.status(400).json({ success: false, message: "Password must be at least 6 characters." });
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ success: false, message: "Please enter a valid email address." });
    }

    // Check if email already exists
    const existing = await pool.query("SELECT id FROM users WHERE email = $1", [email.toLowerCase()]);
    if (existing.rowCount > 0) {
      return res.status(409).json({ success: false, message: "An account with this email already exists." });
    }

    // Hash password
    const saltRounds = 12;
    const password_hash = await bcrypt.hash(password, saltRounds);

    // Insert user
    const result = await pool.query(
      `INSERT INTO users (name, email, password_hash, city, phone, join_date)
       VALUES ($1, $2, $3, $4, $5, NOW())
       RETURNING id, name, email, role, join_date`,
      [name || null, email.toLowerCase(), password_hash, city || null, phone || null]
    );

    const user = result.rows[0];

    res.status(201).json({
      success: true,
      message: "Account created successfully! You can now login.",
      user: { id: user.id, name: user.name, email: user.email },
    });

  } catch (err) {
    console.error("Signup error:", err.message);
    res.status(500).json({ success: false, message: "Server error. Please try again." });
  }
});

// ── POST /api/login ───────────────────────────────────────
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ success: false, message: "Email and password are required." });
    }

    // Find user
    const result = await pool.query(
      "SELECT * FROM users WHERE email = $1",
      [email.toLowerCase()]
    );

    if (result.rowCount === 0) {
      return res.status(401).json({ success: false, message: "No account found with this email." });
    }

    const user = result.rows[0];

    if (!user.is_active) {
      return res.status(403).json({ success: false, message: "Your account has been deactivated. Contact admin." });
    }

    // Check password
    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: "Incorrect password." });
    }

    // Generate JWT
    const token = jwt.sign(
      { id: user.id, email: user.email, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || "7d" }
    );

    // Calculate expiry date
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    // Save session to DB
    const ip = req.ip || req.connection.remoteAddress || "unknown";
    const ua = req.headers["user-agent"] || "unknown";

    await pool.query(
      `INSERT INTO sessions (user_id, token, ip_address, user_agent, created_at, expires_at, is_active)
       VALUES ($1, $2, $3, $4, NOW(), $5, true)`,
      [user.id, token, ip, ua, expiresAt]
    );

    // Update login stats
    await pool.query(
      "UPDATE users SET last_login = NOW(), login_count = login_count + 1 WHERE id = $1",
      [user.id]
    );

    res.json({
      success: true,
      message: "Login successful!",
      token,
      user: {
        id:        user.id,
        name:      user.name,
        email:     user.email,
        role:      user.role,
        join_date: user.join_date,
      },
    });

  } catch (err) {
    console.error("Login error:", err.message);
    res.status(500).json({ success: false, message: "Server error. Please try again." });
  }
});

// ── POST /api/logout ──────────────────────────────────────
router.post("/logout", async (req, res) => {
  try {
    const { email } = req.body;
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith("Bearer ")) {
      const token = authHeader.split(" ")[1];
      // Invalidate the specific session token
      await pool.query(
        "UPDATE sessions SET is_active = false WHERE token = $1",
        [token]
      );
    } else if (email) {
      // Fallback: invalidate all sessions for this email
      const userRes = await pool.query("SELECT id FROM users WHERE email = $1", [email.toLowerCase()]);
      if (userRes.rowCount > 0) {
        await pool.query(
          "UPDATE sessions SET is_active = false WHERE user_id = $1 AND is_active = true",
          [userRes.rows[0].id]
        );
      }
    }

    res.json({ success: true, message: "Logged out successfully." });

  } catch (err) {
    console.error("Logout error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/me — get current user profile ────────────────
router.get("/me", verifyToken, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id, name, email, role, city, phone, avatar_url, join_date, last_login, login_count
       FROM users WHERE id = $1`,
      [req.user.id]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ success: false, message: "User not found." });
    }

    // Get enrolled courses
    const enrollments = await pool.query(
      "SELECT course_slug, progress, completed, enrolled_at FROM enrollments WHERE user_id = $1",
      [req.user.id]
    );

    res.json({
      success: true,
      user: result.rows[0],
      enrollments: enrollments.rows,
    });
  } catch (err) {
    console.error("Profile error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── PUT /api/profile — update profile ────────────────────
router.put("/profile", verifyToken, async (req, res) => {
  try {
    const { name, city, phone, avatar_url } = req.body;

    await pool.query(
      `UPDATE users SET name = COALESCE($1, name), city = COALESCE($2, city),
       phone = COALESCE($3, phone), avatar_url = COALESCE($4, avatar_url)
       WHERE id = $5`,
      [name || null, city || null, phone || null, avatar_url || null, req.user.id]
    );

    res.json({ success: true, message: "Profile updated successfully." });
  } catch (err) {
    console.error("Profile update error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── PUT /api/change-password ──────────────────────────────
router.put("/change-password", verifyToken, async (req, res) => {
  try {
    const { currentPassword, newPassword } = req.body;

    if (!currentPassword || !newPassword) {
      return res.status(400).json({ success: false, message: "Both current and new passwords are required." });
    }
    if (newPassword.length < 6) {
      return res.status(400).json({ success: false, message: "New password must be at least 6 characters." });
    }

    const result = await pool.query("SELECT password_hash FROM users WHERE id = $1", [req.user.id]);
    const isMatch = await bcrypt.compare(currentPassword, result.rows[0].password_hash);

    if (!isMatch) {
      return res.status(401).json({ success: false, message: "Current password is incorrect." });
    }

    const newHash = await bcrypt.hash(newPassword, 12);
    await pool.query("UPDATE users SET password_hash = $1 WHERE id = $2", [newHash, req.user.id]);

    // Invalidate all sessions (force re-login)
    await pool.query("UPDATE sessions SET is_active = false WHERE user_id = $1", [req.user.id]);

    res.json({ success: true, message: "Password changed successfully. Please login again." });
  } catch (err) {
    console.error("Change password error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

module.exports = router;
