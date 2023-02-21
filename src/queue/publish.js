const amqplib = require("amqplib");
require("dotenv").config();

const amqpUrlCloud = "amqps://dtygniff:W2BTz_bjGoQ9oVy1kjwcQDmU_QNhjzho@armadillo.rmq.cloudamqp.com/dtygniff";

const sendMessage = async ({msg}) => {
  try {
    const conn = await amqplib.connect(amqpUrlCloud);

    const channel = await conn.createChannel();

    const exchangeName = "testPublish";

    await channel.assertQueue(exchangeName, 'fanout', {
      durable: false, // tính bền bỉ quyết định channel có bị xóa khi máy chủ mất kết nối hay ko
    });

    await channel.publish(exchangeName, '' , Buffer.from(msg))

  } catch (error) {
    console.log(`[x] Error: ${error}`);
  }
};

sendMessage({msg: "Hello"})

module.exports = sendMessage;
