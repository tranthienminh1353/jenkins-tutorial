const mongoose = require("mongoose");
const Schema = mongoose.Schema;
const slug = require("mongoose-slug-generator");
const mongooseDelete = require("mongoose-delete");

const User = new Schema(
  {
    username: { type: String, unique: true },
    password: { type: String },
  },
  {
    timestamps: true,
  }
);

// Add plugin
mongoose.plugin(slug);
User.plugin(mongooseDelete, { overrideMethods: "all", deletedAt: true });

module.exports = mongoose.model("User", User);
