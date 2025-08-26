package com.example.cs25batch.sender;

import com.example.cs25batch.batch.dto.MailDto;
import io.github.bucket4j.Bucket;

public interface MailSenderStrategy {
    void sendQuizMail(MailDto mailDto);

    Bucket getBucket();
}
