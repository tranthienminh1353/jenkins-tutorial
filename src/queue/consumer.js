const amqplib = require("amqplib");
require("dotenv").config();

const amqpUrlCloud = "amqps://dtygniff:W2BTz_bjGoQ9oVy1kjwcQDmU_QNhjzho@armadillo.rmq.cloudamqp.com/dtygniff";

const receiveQueue = async () => {
  try {
    const conn = await amqplib.connect(amqpUrlCloud);

    const channel = await conn.createChannel();

    const queueName = "test";

    await channel.assertQueue(queueName, {
      durable: true,
    });

    await channel.consume(
      queueName,
      async (msg) => {
        console.log(msg.content.toString());
      },
      {
        noAck: true, // xác nhận consumer đã nhận message hay chưa
      }
    );
  } catch (error) {
    console.log(error);
  }
};

receiveQueue()

module.exports = receiveQueue;
