const express = require("express");
const morgan = require("morgan");
const path = require("path");
const app = express();
const port = 3000;
const db = require("./config/db");
const methodOverride = require("method-override");
require("dotenv").config();

// Connect db
db.connect();

const route = require("./routes");

app.use(express.static(path.join(__dirname, "public")));

app.use(
  express.urlencoded({
    extended: true,
  })
);
app.use(express.json());

app.use(methodOverride("_method"));

// Http logger 123
app.use(morgan("combined"));

// route
route(app);

app.listen(port, () => {
  console.log(`App listening on port ${port}`);
});
