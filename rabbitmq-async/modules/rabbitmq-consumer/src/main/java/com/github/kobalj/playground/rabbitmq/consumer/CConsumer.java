/*
 * Copyright (c) 2016, Jure Kobal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.kobalj.playground.rabbitmq.consumer;

import com.github.kobalj.playground.rabbitmq.lib.CContent;
import com.github.kobalj.playground.rabbitmq.lib.CConverter;
import com.github.kobalj.playground.rabbitmq.lib.CResponse;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 *
 * @author Jure Kobal
 */
public class CConsumer {

    private final String QUEUE_NAME = "test";

    public void consume() throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("<server>");
        factory.setUsername("<usser>");
        factory.setPassword("<pass>");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(QUEUE_NAME, false, consumer);
        
        CConverter converter = new CConverter();

        while (true) {
            String response = null;

            QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            AMQP.BasicProperties props = delivery.getProperties();
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                    .correlationId(props.getCorrelationId())
                    .build();

            try {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(message);
                CContent content = converter.json2Java(message, CContent.class);

                // simulating worload
                Thread.sleep(1000);
                
                CResponse res = new CResponse();
                res.setMsgId(content.getMsgId());
                res.setStatus(true);
                
                response = converter.java2Json(res);
            } catch (Exception e) {
                response = "Error";
            } finally {
                channel.basicPublish("", props.getReplyTo(), replyProps, response.getBytes("UTF-8"));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }

        }

    }
}
