package com.example.cs25batch.batch.component.writer;

import com.example.cs25batch.adapter.RedisStreamsClient;
import com.example.cs25batch.batch.dto.MailDto;
import com.example.cs25batch.sender.context.MailSenderContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailWriter implements ItemWriter<MailDto> {

    private final MailSenderContext mailSenderContext;
    private final RedisStreamsClient streamsClient;

    @Value("${mail.strategy:javaBatchMailSender}")
    private String strategyKey;

    @Override
    public void write(Chunk<? extends MailDto> items) throws Exception {
        for (MailDto mail : items) {
            try {
                //long start = System.currentTimeMillis();
                mailSenderContext.send(mail, strategyKey);
                //long end = System.currentTimeMillis();
                //log.info("[6. 메일 발송] email : {}ms", end - start);
            } catch (Exception e) {
                // 에러 로깅 또는 알림 처리
                System.err.println("메일 발송 실패: " + e.getMessage());
            } finally {
                streamsClient.ackAndDel(mail.getRecordId());
            }
        }
    }
}
