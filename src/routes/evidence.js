const express = require("express");
const router = express.Router();
const evidenceBeforeController = require("../public/app/controllers/evidenceController");

router.get("/", evidenceBeforeController.findAll);
router.post("/", evidenceBeforeController.addNew);

module.exports = router;
