package com.example.freegpt;

import java.util.List;


public class OpenAIRequest  {

    String model;
    List<Message> messages;

    OpenAIRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
}