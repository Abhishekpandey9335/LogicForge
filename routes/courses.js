// routes/courses.js — Course enrollment & progress tracking
const express = require("express");
const router  = express.Router();
const pool    = require("../config/db");
const { verifyToken } = require("../middleware/auth");

// Available courses
const COURSES = ["java", "dsa", "web", "interview"];

// ── POST /api/courses/enroll ──────────────────────────────
router.post("/enroll", verifyToken, async (req, res) => {
  try {
    const { course_slug } = req.body;

    if (!COURSES.includes(course_slug)) {
      return res.status(400).json({ success: false, message: "Invalid course slug." });
    }

    // Insert or do nothing if already enrolled
    await pool.query(
      `INSERT INTO enrollments (user_id, course_slug)
       VALUES ($1, $2)
       ON CONFLICT (user_id, course_slug) DO NOTHING`,
      [req.user.id, course_slug]
    );

    res.json({ success: true, message: `Enrolled in ${course_slug} successfully!` });
  } catch (err) {
    console.error("Enroll error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── PUT /api/courses/progress ─────────────────────────────
router.put("/progress", verifyToken, async (req, res) => {
  try {
    const { course_slug, progress } = req.body;

    if (!COURSES.includes(course_slug)) {
      return res.status(400).json({ success: false, message: "Invalid course slug." });
    }
    if (typeof progress !== "number" || progress < 0 || progress > 100) {
      return res.status(400).json({ success: false, message: "Progress must be a number between 0 and 100." });
    }

    const completed = progress >= 100;

    await pool.query(
      `UPDATE enrollments SET progress = $1, completed = $2
       WHERE user_id = $3 AND course_slug = $4`,
      [progress, completed, req.user.id, course_slug]
    );

    res.json({ success: true, message: "Progress updated.", completed });
  } catch (err) {
    console.error("Progress update error:", err.message);
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── GET /api/courses/my ───────────────────────────────────
router.get("/my", verifyToken, async (req, res) => {
  try {
    const result = await pool.query(
      "SELECT * FROM enrollments WHERE user_id = $1 ORDER BY enrolled_at DESC",
      [req.user.id]
    );
    res.json({ success: true, courses: result.rows });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

module.exports = router;
