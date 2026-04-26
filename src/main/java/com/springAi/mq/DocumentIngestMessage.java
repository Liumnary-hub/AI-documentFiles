package com.springAi.mq;

import java.io.Serializable;

public record DocumentIngestMessage(
        String workspaceId,
        String documentId
) implements Serializable {
}
