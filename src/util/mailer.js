const nodemailer = require("nodemailer");
require("dotenv").config();

const USER = process.env.EMAIL_USER;
const PASSWORD = process.env.EMAIL_PASSWORD;

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: USER,
    pass: PASSWORD,
  },
});

module.exports = transporter;
