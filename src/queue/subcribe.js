const amqplib = require("amqplib");
require("dotenv").config();

const amqpUrlCloud = "amqps://dtygniff:W2BTz_bjGoQ9oVy1kjwcQDmU_QNhjzho@armadillo.rmq.cloudamqp.com/dtygniff";

const reciveMessage = async () => {
  try {
    const conn = await amqplib.connect(amqpUrlCloud);

    const channel = await conn.createChannel();

    const exchangeName = "testPublish";

    await channel.assertExchange(exchangeName, 'fanout', {
      durable: true, // tính bền bỉ quyết định channel có bị xóa khi máy chủ mất kết nối hay ko
    });

    const {
        queue
    } = await channel.assertQueue('', {
        exclusive: true // Tự động xóa channel khi không được subcribe nữa
    })

    console.log(`[x] Channel ${queue}`);

    await channel.bindQueue(queue, exchangeName, "")

    await channel.consume(queue, msg => {
        console.log(`[x] Message: ${msg.content.toString()}`);
    }, {
        noAck: true // xác nhận consumer đã nhận message hay chưa
    })

  } catch (error) {
    console.log(error);
  }
};

reciveMessage()

module.exports = reciveMessage;
