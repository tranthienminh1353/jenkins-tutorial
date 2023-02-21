const transferService = require("../service/transferDataService");
const EvidenceBefore = require("../models/mysql/EvidenceBefore");
const dateUtil = require("../../../util/dateUtil");
const producer = require("../../../queue/producer");

exports.findAll = async (req, res) => {
  const transfer = await transferService.transferMySQLtoMongo(2);
  res.send(transfer);
};

exports.addNew = async (req, res) => {
  const obj = {
    description: `des ${Number(Math.floor(Math.random() * 500))}`,
    comment: `cmt ${Number(Math.floor(Math.random() * 500))}`,
    airway_bill: Number(Math.floor(Math.random() * 500)),
    create_date: dateUtil.formatDateTimeRes(new Date()),
    update_date: dateUtil.formatDateTimeRes(new Date()),
    create_by: "admin",
    update_by: "admin",
  };
  const [e, ...rest] = await EvidenceBefore.save(obj);
  producer(obj);
  res.send(e);
};


