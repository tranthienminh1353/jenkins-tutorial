const jwtVariable = require("../../../util/jwt");
const User = require("../models/mongo/User");
const tokenUtil = require("../../../util/tokenUtil");
const message = require("../../../util/message");

exports.isAuth = async (req, res, next) => {
  const accessTokenFromHeader = req.headers.x_authorization;
  if (!accessTokenFromHeader) {
    return res.status(401).send(message.AUTHENTICATION.ACCESS_TOKEN_NOT_FOUND);
  }

  const accessTokenSecret =
    process.env.ACCESS_TOKEN_SECRET || jwtVariable.accessTokenSecret;

  const verified = await tokenUtil.verifyToken(
    accessTokenFromHeader,
    accessTokenSecret
  );
  if (!verified) {
    return res.status(403).send(message.AUTHORiZATION.DO_NOT_PERMISSION);
  }

  const user = await User.findOne({ username: verified.payload.username });
  req.user = user;

  return next();
};
