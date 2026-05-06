// routes/newsletter.js — Newsletter subscribe + Contact form
const express = require("express");
const router  = express.Router();
const pool    = require("../config/db");

// ── POST /api/newsletter/subscribe ───────────────────────
router.post("/subscribe", async (req, res) => {
  try {
    const { email } = req.body;

    if (!email || !email.includes("@")) {
      return res.status(400).json({ success: false, message: "Please enter a valid email address." });
    }

    // Check if already subscribed
    const existing = await pool.query(
      "SELECT id, is_active FROM newsletter_subscribers WHERE email = $1",
      [email.toLowerCase()]
    );

    if (existing.rowCount > 0) {
      if (existing.rows[0].is_active) {
        return res.json({ success: true, message: "You're already subscribed! 🎉" });
      }
      // Re-subscribe
      await pool.query(
        "UPDATE newsletter_subscribers SET is_active = true, subscribed_at = NOW() WHERE email = $1",
        [email.toLowerCase()]
      );
      return res.json({ success: true, message: "Welcome back! You've been re-subscribed. 📬" });
    }

    await pool.query(
      "INSERT INTO newsletter_subscribers (email) VALUES ($1)",
      [email.toLowerCase()]
    );

    res.status(201).json({ success: true, message: "Subscribed successfully! Check your inbox. 🎉" });

  } catch (err) {
    console.error("Newsletter subscribe error:", err.message);
    res.status(500).json({ success: false, message: "Server error. Please try again." });
  }
});

// ── POST /api/newsletter/unsubscribe ─────────────────────
router.post("/unsubscribe", async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) return res.status(400).json({ success: false, message: "Email is required." });

    await pool.query(
      "UPDATE newsletter_subscribers SET is_active = false WHERE email = $1",
      [email.toLowerCase()]
    );

    res.json({ success: true, message: "You have been unsubscribed." });
  } catch (err) {
    res.status(500).json({ success: false, message: "Server error." });
  }
});

// ── POST /api/contact ──────────────────────────────────────
router.post("/contact", async (req, res) => {
  try {
    const { name, email, message } = req.body;

    if (!email || !message) {
      return res.status(400).json({ success: false, message: "Email and message are required." });
    }

    await pool.query(
      "INSERT INTO contact_messages (name, email, message) VALUES ($1, $2, $3)",
      [name || "Anonymous", email.toLowerCase(), message]
    );

    res.status(201).json({ success: true, message: "Message sent! We'll reply within 24 hours." });
  } catch (err) {
    console.error("Contact form error:", err.message);
    res.status(500).json({ success: false, message: "Server error. Please try again." });
  }
});

module.exports = router;
