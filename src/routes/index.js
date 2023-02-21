const authRouter = require("./auth");
const evidenceRouter = require("./evidence");
const authMiddleware = require("../public/app/middleware/authMiddleware");
const isAuth = authMiddleware.isAuth;

function route(app) {
  app.use("/auth", authRouter);
  app.use("/evidence", evidenceRouter);
}

module.exports = route;
