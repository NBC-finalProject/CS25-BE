package com.example.cs25batch.sender;

import com.example.cs25batch.batch.dto.MailDto;

public interface MailSenderStrategy {
    void sendQuizMail(MailDto mailDto);

    boolean tryConsume(Long num);
}
