const db = require("../../../../config/mysql");

class EvidenceBefore {
  constructor() {}

  async findAll() {
    const query =
      "SELECT e.* FROM evidence_before_post e join file_storage f on e.id = f.evidence_before_post_id group by e.id;";
    return await db.execute(query);
  }
  async findById(id) {
    const query = `SELECT * FROM evidence_before_post where id = ${id};`;
    return await db.execute(query);
  }
  async findFileStorageByEvidenceBeforeId(id) {
    const query = `SELECT * FROM file_storage where evidence_before_post_id = ${id};`;
    return await db.execute(query);
  }
  async save({ ...params }) {
    const {
      description,
      comment,
      airway_bill,
      create_date,
      update_date,
      create_by,
      update_by,
    } = params;
    const query =
      "INSERT INTO `evidence_before_post` (`description`, `comment`, `airway_bill`, `create_date`, `update_date`, `create_by`, `update_by`) " +
      `VALUES ("${description}", "${comment}", ${airway_bill}, "${create_date}", "${update_date}", "${create_by}", "${update_by}");`;
    return await db.execute(query);
  }
}

module.exports = new EvidenceBefore();
