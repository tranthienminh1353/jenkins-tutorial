const EvidenceBefore = require("../models/mysql/EvidenceBefore");

const evidenceBeforeService = {
  async findEvidenceBeforeById(id) {
    const [[evidenceBefore], ...evidenceBeforeRest] =
      await EvidenceBefore.findById(id);
    if (evidenceBefore) {
      const [fileStorages, ...fileStorageRest] =
        await EvidenceBefore.findFileStorageByEvidenceBeforeId(
          evidenceBefore.id
        );
      const obj = {
        ...evidenceBefore,
        fileStorage: [...fileStorages],
      };
      return obj;
    } else {
      return undefined;
    }
  },
};

module.exports = evidenceBeforeService;
