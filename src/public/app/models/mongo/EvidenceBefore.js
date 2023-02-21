const mongoose = require("mongoose");
const Schema = mongoose.Schema;
const ObjectId = Schema.ObjectId;

const EvidenceBefore = new Schema({
  description: { type: String },
  comment: { type: String },
  airway_bill: { type: Number },
  create_date: { type: Date },
  update_date: { type: Date },
  create_by: { type: String },
  update_by: { type: String },
  file_storage: {
    type: [
      {
        name: { type: String },
        type: { type: String },
        create_date: { type: Date },
        progress_id: { type: Number },
        path_file_server: { type: String },
        post_invoice_id: { type: Number },
        post_package_id: { type: Number },
        evidence_before_post_id: { type: Number },
        post_evidence_after_id: { type: Number },
        is_choosen: { type: Boolean },
        construction_id: { type: Number },
      },
    ],
  },
});

module.exports = mongoose.model(
  "EvidenceBefore",
  EvidenceBefore,
  "evidence_before_post"
);
