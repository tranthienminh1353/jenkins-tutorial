const amqplib = require("amqplib");
require("dotenv").config();

const amqpUrlCloud = "amqps://dtygniff:W2BTz_bjGoQ9oVy1kjwcQDmU_QNhjzho@armadillo.rmq.cloudamqp.com/dtygniff";

const sendQueue = async ({msg}) => {
  try {
    const conn = await amqplib.connect(amqpUrlCloud);

    const channel = await conn.createChannel();

    const queueName = "test";

    await channel.assertQueue(queueName, {
      durable: true, // tính bền bỉ quyết định channel có bị xóa khi máy chủ mất kết nối hay ko
    });
    await channel.sendToQueue(queueName, Buffer.from(msg), {
      persistent: true, // lưu lại message khi mất kết nối máy chủ (catch/ổ cứng)
    });
  } catch (error) {
    console.log(error);
  }
};

sendQueue({msg: "Hello"})

module.exports = sendQueue;
