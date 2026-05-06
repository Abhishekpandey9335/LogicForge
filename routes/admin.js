// routes/admin.js — Full Admin Panel API
const express = require("express");
const router  = express.Router();
const bcrypt  = require("bcryptjs");
const pool    = require("../config/db");
const { verifyAdmin } = require("../middleware/auth");

// All admin routes are protected by verifyAdmin middleware
router.use(verifyAdmin);

// ── GET /api/admin/dashboard ──────────────────────────────
// Returns overall platform statistics
router.get("/dashboard", async (req, res) => {
  try {
    const [
      totalUsers,
      activeUsers,
      newUsersToday,
      newUsersThisWeek,
      newUsersThisMonth,
      activeSessions,
      totalEnrollments,
      totalSubscribers,
      unreadMessages,
      totalLogins,
    ] = await Promise.all([
      pool.query("SELECT COUNT(*) FROM users"),
      pool.query("SELECT COUNT(*) FROM users WHERE is_active = true"),
      pool.query("SELECT COUNT(*) FROM users WHERE join_date >= CURRENT_DATE"),
      pool.query("SELECT COUNT(*) FROM users WHERE join_date >= NOW() - INTERVAL '7 days'"),
      pool.query("SELECT COUNT(*) FROM users WHERE join_date >= NOW() - INTERVAL '30 days'"),
      pool.query("SELECT COUNT(*) FROM sessions WHERE is_active = true AND expires_at > NOW()"),
      pool.query("SELECT COUNT(*) FROM enrollments"),
      pool.query("SELECT COUNT(*) FROM newsletter_subscribers WHERE is_active = true"),
      pool.query("SELECT COUNT(*) FROM contact_messages WHERE is_read = false"),
      pool.query("SELECT COALESCE(SUM(login_count),0) as total FROM users"),
    ]);

    // Enrollments breakdown per course
    const courseStats = await pool.query(
      `SELECT course_slug, COUNT(*) as enrolled, 
       ROUND(AVG(progress)) as avg_progress,
       COUNT(*) FILTER (WHERE completed = true) as completed
       FROM enrollments GROUP BY course_slug ORDER BY enrolled DESC`
    );

    // Daily signups for last 14 days
    const signupTrend = await pool.query(
      `SELECT DATE(join_date) as date, COUNT(*) as count
       FROM users
       WHERE join_date >= NOW() - INTERVAL '14 days'
       GROUP BY DATE(join_date)
       ORDER BY date ASC`
    );

    // Daily logins for last 14 days
    const loginTrend = await pool.query(
      `SELECT DATE(created_at) as date, COUNT(*) as count
       FROM sessions
       WHERE created_at >= NOW() - INTERVAL '14 days'
       GROUP BY DATE(created_at)
       ORDER BY date ASC`
    );

    res.json({
      success: true,
      stats: {
        users: {
          total:        parseInt(totalUsers.rows[0].count),
          active:       parseInt(activeUsers.rows[0].count),
          new_today:    parseInt(newUsersToday.rows[0].count),
          new_week:     parseInt(newUsersThisWeek.rows[0].count),
          new_month:    parseInt(newUsersThisMonth.rows[0].count),
          total_logins: parseInt(totalLogins.rows[0].total),
        },
        sessions: {
          currently_active: parseInt(activeSessions.rows[0].count),
        },
        enrollments: {
          total: parseInt(totalEnrollments.rows[0].count),
          by_course: courseStats.rows,
        },
        newsletter: {
          subscribers: parseInt(totalSubscribers.rows[0].count),
        },
        messages: {
          unread: parseInt(unreadMessages.rows[0].count),
        },
      },
      charts: {
        signup_trend: signupTrend.rows,
        login_trend:  loginTrend.rows,
      },
    });
  } catch (err) {
    console.error("Admin dashboard error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/admin/users ──────────────────────────────────
// List all users with filters & pagination
router.get("/users", async (req, res) => {
  try {
    const {
      page = 1,
      limit = 20,
      search = "",
      sort = "join_date",
      order = "desc",
      status = "all",
    } = req.query;

    const offset = (parseInt(page) - 1) * parseInt(limit);

    const allowedSort  = ["join_date", "last_login", "name", "email", "login_count"];
    const allowedOrder = ["asc", "desc"];
    const safeSort  = allowedSort.includes(sort)  ? sort  : "join_date";
    const safeOrder = allowedOrder.includes(order) ? order : "desc";

    let whereClause = "WHERE 1=1";
    const params = [];

    if (search) {
      params.push(`%${search.toLowerCase()}%`);
      whereClause += ` AND (LOWER(email) LIKE $${params.length} OR LOWER(name) LIKE $${params.length})`;
    }
    if (status === "active")   whereClause += " AND is_active = true";
    if (status === "inactive") whereClause += " AND is_active = false";
    if (status === "online") {
      whereClause += ` AND id IN (
        SELECT DISTINCT user_id FROM sessions WHERE is_active = true AND expires_at > NOW()
      )`;
    }

    const countResult = await pool.query(`SELECT COUNT(*) FROM users ${whereClause}`, params);

    params.push(parseInt(limit), offset);
    const usersResult = await pool.query(
      `SELECT id, name, email, role, is_active, city, join_date, last_login, login_count,
              (SELECT COUNT(*) FROM sessions WHERE user_id = users.id AND is_active = true AND expires_at > NOW()) as active_sessions
       FROM users ${whereClause}
       ORDER BY ${safeSort} ${safeOrder}
       LIMIT $${params.length - 1} OFFSET $${params.length}`,
      params
    );

    res.json({
      success: true,
      users:      usersResult.rows,
      total:      parseInt(countResult.rows[0].count),
      page:       parseInt(page),
      totalPages: Math.ceil(parseInt(countResult.rows[0].count) / parseInt(limit)),
    });
  } catch (err) {
    console.error("Admin users error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/admin/users/:id ──────────────────────────────
// Get detailed info on one user
router.get("/users/:id", async (req, res) => {
  try {
    const { id } = req.params;

    const userResult = await pool.query(
      `SELECT id, name, email, role, is_active, city, phone, avatar_url,
              join_date, last_login, login_count
       FROM users WHERE id = $1`,
      [id]
    );

    if (userResult.rowCount === 0) {
      return res.status(404).json({ success: false, message: "User not found." });
    }

    const sessions = await pool.query(
      `SELECT id, ip_address, user_agent, created_at, expires_at, is_active
       FROM sessions WHERE user_id = $1 ORDER BY created_at DESC LIMIT 10`,
      [id]
    );

    const enrollments = await pool.query(
      "SELECT course_slug, progress, completed, enrolled_at FROM enrollments WHERE user_id = $1",
      [id]
    );

    res.json({
      success: true,
      user:        userResult.rows[0],
      sessions:    sessions.rows,
      enrollments: enrollments.rows,
    });
  } catch (err) {
    console.error("Admin user detail error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── PUT /api/admin/users/:id ──────────────────────────────
// Update user details (admin can edit name, role, status, etc.)
router.put("/users/:id", async (req, res) => {
  try {
    const { id } = req.params;
    const { name, role, is_active, city, phone } = req.body;

    await pool.query(
      `UPDATE users SET
         name      = COALESCE($1, name),
         role      = COALESCE($2, role),
         is_active = COALESCE($3, is_active),
         city      = COALESCE($4, city),
         phone     = COALESCE($5, phone)
       WHERE id = $6`,
      [name, role, is_active, city, phone, id]
    );

    await logAdminAction(`Updated user #${id}`, `Fields: ${JSON.stringify(req.body)}`);

    res.json({ success: true, message: "User updated successfully." });
  } catch (err) {
    console.error("Admin update user error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── DELETE /api/admin/users/:id ───────────────────────────
router.delete("/users/:id", async (req, res) => {
  try {
    const { id } = req.params;
    await pool.query("DELETE FROM users WHERE id = $1", [id]);
    await logAdminAction(`Deleted user #${id}`, null);
    res.json({ success: true, message: "User deleted successfully." });
  } catch (err) {
    console.error("Admin delete user error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── POST /api/admin/users/:id/toggle-status ───────────────
// Activate / Deactivate a user account
router.post("/users/:id/toggle-status", async (req, res) => {
  try {
    const { id } = req.params;
    const result = await pool.query(
      "UPDATE users SET is_active = NOT is_active WHERE id = $1 RETURNING is_active, name",
      [id]
    );
    const { is_active, name } = result.rows[0];

    if (!is_active) {
      // Invalidate all sessions for this user
      await pool.query("UPDATE sessions SET is_active = false WHERE user_id = $1", [id]);
    }

    await logAdminAction(
      `${is_active ? "Activated" : "Deactivated"} user #${id}`,
      `User: ${name}`
    );

    res.json({
      success: true,
      is_active,
      message: `User ${is_active ? "activated" : "deactivated"} successfully.`,
    });
  } catch (err) {
    console.error("Toggle status error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── POST /api/admin/users/:id/reset-password ─────────────
router.post("/users/:id/reset-password", async (req, res) => {
  try {
    const { id } = req.params;
    const { newPassword } = req.body;

    if (!newPassword || newPassword.length < 6) {
      return res.status(400).json({ success: false, message: "New password must be at least 6 characters." });
    }

    const hash = await bcrypt.hash(newPassword, 12);
    await pool.query("UPDATE users SET password_hash = $1 WHERE id = $2", [hash, id]);
    await pool.query("UPDATE sessions SET is_active = false WHERE user_id = $1", [id]);

    await logAdminAction(`Reset password for user #${id}`, null);

    res.json({ success: true, message: "Password reset. User must login again." });
  } catch (err) {
    console.error("Reset password error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/admin/sessions ───────────────────────────────
// View all currently active sessions
router.get("/sessions", async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT s.id, s.ip_address, s.user_agent, s.created_at, s.expires_at,
              u.id as user_id, u.name, u.email
       FROM sessions s
       JOIN users u ON s.user_id = u.id
       WHERE s.is_active = true AND s.expires_at > NOW()
       ORDER BY s.created_at DESC
       LIMIT 100`
    );

    res.json({ success: true, sessions: result.rows, total: result.rowCount });
  } catch (err) {
    console.error("Admin sessions error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── DELETE /api/admin/sessions/:id ───────────────────────
// Force-logout a specific session
router.delete("/sessions/:id", async (req, res) => {
  try {
    await pool.query("UPDATE sessions SET is_active = false WHERE id = $1", [req.params.id]);
    await logAdminAction(`Force-logged out session #${req.params.id}`, null);
    res.json({ success: true, message: "Session terminated." });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── DELETE /api/admin/sessions/user/:userId ───────────────
// Force-logout ALL sessions for a user
router.delete("/sessions/user/:userId", async (req, res) => {
  try {
    const result = await pool.query(
      "UPDATE sessions SET is_active = false WHERE user_id = $1 AND is_active = true RETURNING id",
      [req.params.userId]
    );
    await logAdminAction(`Force-logged out all sessions for user #${req.params.userId}`, `${result.rowCount} sessions ended`);
    res.json({ success: true, message: `${result.rowCount} sessions terminated.` });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/admin/subscribers ────────────────────────────
router.get("/subscribers", async (req, res) => {
  try {
    const { page = 1, limit = 50 } = req.query;
    const offset = (parseInt(page) - 1) * parseInt(limit);

    const result = await pool.query(
      `SELECT * FROM newsletter_subscribers ORDER BY subscribed_at DESC LIMIT $1 OFFSET $2`,
      [parseInt(limit), offset]
    );
    const countResult = await pool.query("SELECT COUNT(*) FROM newsletter_subscribers");

    res.json({
      success: true,
      subscribers: result.rows,
      total: parseInt(countResult.rows[0].count),
    });
  } catch (err) {
    console.error("Admin subscribers error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── DELETE /api/admin/subscribers/:id ────────────────────
router.delete("/subscribers/:id", async (req, res) => {
  try {
    await pool.query("UPDATE newsletter_subscribers SET is_active = false WHERE id = $1", [req.params.id]);
    res.json({ success: true, message: "Subscriber removed." });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/admin/messages ───────────────────────────────
router.get("/messages", async (req, res) => {
  try {
    const result = await pool.query(
      "SELECT * FROM contact_messages ORDER BY created_at DESC"
    );
    res.json({ success: true, messages: result.rows, total: result.rowCount });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── PUT /api/admin/messages/:id/read ─────────────────────
router.put("/messages/:id/read", async (req, res) => {
  try {
    await pool.query("UPDATE contact_messages SET is_read = true WHERE id = $1", [req.params.id]);
    res.json({ success: true, message: "Marked as read." });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/admin/announcements ──────────────────────────
router.get("/announcements", async (req, res) => {
  try {
    const result = await pool.query("SELECT * FROM announcements ORDER BY created_at DESC");
    res.json({ success: true, announcements: result.rows });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── POST /api/admin/announcements ────────────────────────
router.post("/announcements", async (req, res) => {
  try {
    const { message, link_text, link_url } = req.body;
    if (!message) return res.status(400).json({ success: false, message: "Message is required." });

    // Deactivate all previous announcements first
    await pool.query("UPDATE announcements SET is_active = false");

    const result = await pool.query(
      `INSERT INTO announcements (message, link_text, link_url, is_active)
       VALUES ($1, $2, $3, true) RETURNING *`,
      [message, link_text || null, link_url || null]
    );

    await logAdminAction("Created announcement", message);
    res.json({ success: true, announcement: result.rows[0] });
  } catch (err) {
    console.error("Create announcement error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── DELETE /api/admin/announcements/:id ──────────────────
router.delete("/announcements/:id", async (req, res) => {
  try {
    await pool.query("DELETE FROM announcements WHERE id = $1", [req.params.id]);
    res.json({ success: true, message: "Announcement deleted." });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/admin/logs ───────────────────────────────────
router.get("/logs", async (req, res) => {
  try {
    const result = await pool.query(
      "SELECT * FROM admin_logs ORDER BY performed_at DESC LIMIT 100"
    );
    res.json({ success: true, logs: result.rows });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── Helper: log admin actions ─────────────────────────────
async function logAdminAction(action, details) {
  try {
    await pool.query(
      "INSERT INTO admin_logs (action, details) VALUES ($1, $2)",
      [action, details || null]
    );
  } catch (_) {}
}

module.exports = router;
