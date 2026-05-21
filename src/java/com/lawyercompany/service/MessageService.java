package com.lawyercompany.service;

import com.lawyercompany.dao.MessageDAO;
import com.lawyercompany.entity.Message;
import com.lawyercompany.factory.DAOFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class MessageService {

    private final MessageDAO messageDAO;

    public MessageService() {
        this.messageDAO = DAOFactory.getMessageDAO();
    }

    /**
     * Отправить сообщение в рамках дела.
     *
     * @param caseId   дело
     * @param senderId отправитель (users.id)
     * @param text     текст сообщения
     * @return созданное сообщение
     */
    public Message sendMessage(UUID caseId, UUID senderId, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }
        if (text.length() > 1000) {
            throw new IllegalArgumentException("Сообщение не должно превышать 1000 символов");
        }

        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setCaseId(caseId);
        msg.setSenderId(senderId);
        msg.setMessageText(text);
        msg.setSentAt(OffsetDateTime.now());
        messageDAO.save(msg);

        return msg;
    }

    public List<Message> getMessagesByCase(UUID caseId) {
        return messageDAO.findByCaseId(caseId);
    }

    public Message getMessageById(UUID messageId) {
        return messageDAO.findById(messageId);
    }

    public void deleteMessage(UUID messageId) {
        messageDAO.delete(messageId);
    }
}
