const User = require("../models/mongo/User");
const bcrypt = require("bcrypt");
const saltRounds = 10;
const tokenUtil = require("../../../util/tokenUtil");
const jwtVariable = require("../../../util/jwt");
const randToken = require("rand-token");
const message = require("../../../util/message");
const consumer = require("../../../queue/consumer");

exports.subscribe = async (req, res) => {
  consumer(handleError);
  res.send("");
};

const handleError = (msg) => {
  if (msg === "Success") {
    console.log("Success");
  } else if (msg === "Error") {
    console.log("Error");
  }
};

exports.register = async (req, res) => {
  const username = req.body.username.toLowerCase();
  const user = await User.findOne({ username: username });
  if (user) res.status(409).send(message.REGISTER.REQUIRED_USERNAME);
  else {
    const hashPassword = bcrypt.hashSync(req.body.password, saltRounds);
    const newUser = new User({
      username: username,
      password: hashPassword,
    });
    const createUser = await newUser.save();
    if (!createUser) {
      return res.status(400).send(message.REGISTER.CREATION_ERROR);
    }
    return res.send({
      username,
    });
  }
};

exports.login = async (req, res) => {
  const username = req.body.username.toLowerCase() || "";
  const password = req.body.password || "";

  const user = await User.findOne({ username: username });
  if (!user) {
    return res.status(401).send(message.LOGIN.USERNAME_NOT_FOUND);
  }

  const isPasswordValid = bcrypt.compareSync(password, user.password);
  if (!isPasswordValid) {
    return res.status(401).send(message.LOGIN.INCORRECT_PASSWORD);
  }

  const accessTokenLife = jwtVariable.accessTokenLife;
  const accessTokenSecret = process.env.ACCESS_TOKEN_SECRET || "";
  const refreshTokenSize = jwtVariable.refreshTokenSize;

  const dataForAccessToken = {
    username: user.username,
  };
  const accessToken = await tokenUtil.generateToken(
    dataForAccessToken,
    accessTokenSecret,
    accessTokenLife
  );
  if (!accessToken) {
    return res.status(401).send(message.LOGIN.LOGIN_FAILED);
  }

  let refreshToken = randToken.generate(refreshTokenSize);
  if (!user.refreshToken) {
    await User.findOneAndUpdate(user.username, {
      refreshToken,
    });
  } else {
    refreshToken = user.refreshToken;
  }

  return res.json({
    msg: message.LOGIN.LOGIN_SUCCESS,
    accessToken,
    refreshToken,
  });
};

exports.refreshToken = async (req, res) => {
  // Lấy access token từ header
  const accessTokenFromHeader = req.headers.x_authorization;
  if (!accessTokenFromHeader) {
    return res.status(400).send("Không tìm thấy access token.");
  }

  // Lấy refresh token từ body
  const refreshTokenFromBody = req.body.refreshToken;
  if (!refreshTokenFromBody) {
    return res.status(400).send("Không tìm thấy refresh token.");
  }

  const accessTokenSecret =
    process.env.ACCESS_TOKEN_SECRET || jwtVariable.accessTokenSecret;
  const accessTokenLife =
    process.env.ACCESS_TOKEN_LIFE || jwtVariable.accessTokenLife;

  // Decode access token đó
  const decoded = await authMethod.decodeToken(
    accessTokenFromHeader,
    accessTokenSecret
  );
  if (!decoded) {
    return res.status(400).send("Access token không hợp lệ.");
  }

  const username = decoded.payload.username; // Lấy username từ payload

  const user = await userModel.getUser(username);
  if (!user) {
    return res.status(401).send("User không tồn tại.");
  }

  if (refreshTokenFromBody !== user.refreshToken) {
    return res.status(400).send("Refresh token không hợp lệ.");
  }

  // Tạo access token mới
  const dataForAccessToken = {
    username,
  };

  const accessToken = await authMethod.generateToken(
    dataForAccessToken,
    accessTokenSecret,
    accessTokenLife
  );
  if (!accessToken) {
    return res
      .status(400)
      .send("Tạo access token không thành công, vui lòng thử lại.");
  }
  return res.json({
    accessToken,
  });
};
