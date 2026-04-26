package com.springAi.service;

import com.rabbitmq.client.AMQP;
import com.springAi.config.RabbitConfig;
import com.springAi.mq.DocumentIngestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestDlqService {

    private final RabbitTemplate rabbitTemplate;

    public Map<String, Object> summary() {
        Integer count = rabbitTemplate.execute(channel -> {
            AMQP.Queue.DeclareOk ok = channel.queueDeclarePassive(RabbitConfig.DOC_INGEST_DLX_QUEUE);
            return ok.getMessageCount();
        });
        return Map.of(
                "queue", RabbitConfig.DOC_INGEST_DLX_QUEUE,
                "messageCount", count == null ? 0 : count
        );
    }

    public int retryAll() {
        int retried = 0;
        while (true) {
            Message raw = rabbitTemplate.receive(RabbitConfig.DOC_INGEST_DLX_QUEUE);
            if (raw == null) {
                break;
            }
            DocumentIngestMessage message = safeConvert(raw);
            if (message == null) {
                continue;
            }
            rabbitTemplate.convertAndSend(RabbitConfig.DOC_INGEST_EXCHANGE, RabbitConfig.DOC_INGEST_ROUTING_KEY, message);
            retried++;
        }
        return retried;
    }

    public int retryByDocumentId(String documentId) {
        int retried = 0;
        List<DocumentIngestMessage> keepInDlq = new ArrayList<>();

        while (true) {
            Message raw = rabbitTemplate.receive(RabbitConfig.DOC_INGEST_DLX_QUEUE);
            if (raw == null) {
                break;
            }
            DocumentIngestMessage message = safeConvert(raw);
            if (message == null) {
                continue;
            }
            if (documentId.equals(message.documentId())) {
                rabbitTemplate.convertAndSend(RabbitConfig.DOC_INGEST_EXCHANGE, RabbitConfig.DOC_INGEST_ROUTING_KEY, message);
                retried++;
            } else {
                keepInDlq.add(message);
            }
        }

        for (DocumentIngestMessage message : keepInDlq) {
            rabbitTemplate.convertAndSend(RabbitConfig.DOC_INGEST_DLX_EXCHANGE, RabbitConfig.DOC_INGEST_DLX_ROUTING_KEY, message);
        }

        return retried;
    }

    private DocumentIngestMessage safeConvert(Message raw) {
        try {
            Object payload = rabbitTemplate.getMessageConverter().fromMessage(raw);
            if (payload instanceof DocumentIngestMessage message) {
                return message;
            }
            log.warn("[doc-ingest-dlq] skip unknown payload type: {}", payload == null ? "null" : payload.getClass().getName());
            return null;
        } catch (MessageConversionException e) {
            log.warn("[doc-ingest-dlq] skip invalid message body, conversion failed: {}", e.getMessage());
            return null;
        }
    }
}
