const mysql = require("mysql2");
require("dotenv").config();

const MYSQL_PASSWORD = process.env.DATABASE_MYSQL_PASSWORD;
const DB_NAME = process.env.DATABASE_MYSQL_NAME;

const pool = mysql.createPool({
  host: "localhost",
  user: "root",
  database: DB_NAME,
  password: MYSQL_PASSWORD,
});

module.exports = pool.promise();
