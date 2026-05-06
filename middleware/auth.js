// middleware/auth.js — JWT verification middleware
const jwt = require("jsonwebtoken");
const pool = require("../config/db");

// ── Verify JWT token (standard user) ─────────────────────
const verifyToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ success: false, message: "No token provided. Please login." });
    }

    const token = authHeader.split(" ")[1];

    // Verify signature + expiry
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    // Check session is still active in DB
    const sessionResult = await pool.query(
      `SELECT s.id, s.is_active, s.expires_at, u.id as user_id, u.email, u.name, u.role, u.is_active as user_active
       FROM sessions s
       JOIN users u ON s.user_id = u.id
       WHERE s.token = $1`,
      [token]
    );

    if (sessionResult.rowCount === 0) {
      return res.status(401).json({ success: false, message: "Session not found. Please login again." });
    }

    const session = sessionResult.rows[0];

    if (!session.is_active) {
      return res.status(401).json({ success: false, message: "Session expired. Please login again." });
    }

    if (!session.user_active) {
      return res.status(403).json({ success: false, message: "Account has been deactivated. Contact admin." });
    }

    // Attach user to request
    req.user = {
      id:    session.user_id,
      email: session.email,
      name:  session.name,
      role:  session.role,
    };

    next();
  } catch (err) {
    if (err.name === "TokenExpiredError") {
      return res.status(401).json({ success: false, message: "Token expired. Please login again." });
    }
    return res.status(401).json({ success: false, message: "Invalid token." });
  }
};

// ── Verify admin password (simple header-based check) ─────
const verifyAdmin = (req, res, next) => {
  const adminPassword = req.headers["x-admin-password"];

  if (!adminPassword || adminPassword !== process.env.ADMIN_PASSWORD) {
    return res.status(403).json({ success: false, message: "Invalid admin credentials." });
  }

  next();
};

// ── Optional token (attach user if logged in, else continue) ─
const optionalToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      req.user = null;
      return next();
    }
    const token = authHeader.split(" ")[1];
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded;
    next();
  } catch {
    req.user = null;
    next();
  }
};

module.exports = { verifyToken, verifyAdmin, optionalToken };
