const EvidenceBefore = require("../models/mongo/EvidenceBefore");

const transferDataService = {
  async transferMySQLtoMongo(evidenceBeforeMySQL) {
    const evidenceBeforeMongo = new EvidenceBefore({
      description: evidenceBeforeMySQL.description,
      comment: evidenceBeforeMySQL.comment,
      airway_bill: evidenceBeforeMySQL.airway_bill,
      create_date: evidenceBeforeMySQL.create_date,
      update_date: evidenceBeforeMySQL.update_date,
      create_by: evidenceBeforeMySQL.create_by,
      update_by: evidenceBeforeMySQL.update_by,
      file_storage: evidenceBeforeMySQL.fileStorage
        ? [
            ...evidenceBeforeMySQL.fileStorage.map((data) => {
              return {
                ...data,
              };
            }),
          ]
        : [],
    });

    try {
      await evidenceBeforeMongo.save();
    } catch (error) {
      return {
        message: "Error",
        code: error.code,
      };
    }

    return {
      message: "Success",
    };
  },
};

module.exports = transferDataService;
