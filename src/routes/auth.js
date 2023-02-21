const express = require("express");
const router = express.Router();
const authController = require("../public/app/controllers/AuthController");
const authMiddleware = require("../public/app/middleware/authMiddleware");
const isAuth = authMiddleware.isAuth;

router.post("/register", authController.register);
router.post("/login", authController.login);
router.post("/refresh", isAuth, authController.refreshToken);
router.get("/subscribe", authController.subscribe);

module.exports = router;
