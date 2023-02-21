const mongoose = require("mongoose");
require("dotenv").config();

const DB_NAME = process.env.DATABASE_MONGO_NAME;

const connect = async () => {
  try {
    await mongoose.connect(
      `mongodb+srv://bestwork:bestwork@bestworkcluster.rlqz2vo.mongodb.net/${DB_NAME}`
    );
    console.log(`Connect ${DB_NAME} successfully!!!`);
  } catch (error) {
    console.log(`Connect ${DB_NAME} error: `, error);
  }
};

module.exports = { connect };
