package com.lawyercompany.up.api;

import com.lawyercompany.up.dto.NotificationDTO;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleJson {

    public static String toJson(List<NotificationDTO> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (NotificationDTO n : list) {
            if (!first) sb.append(',');
            first = false;
            sb.append('{');
            sb.append("\"id\":\"").append(n.id).append("\",");
            sb.append("\"type\":\"").append(escape(n.type)).append("\",");
            sb.append("\"createdAt\":\"").append(n.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("\",");
            sb.append("\"caseId\":\"").append(n.caseId).append("\",");
            sb.append("\"message\":\"").append(escape(n.message)).append("\"");
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }


    public static List<NotificationDTO> parseNotifications(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        
        List<NotificationDTO> parsed = parseArray(json);
        return parsed; 
    }

    private static List<NotificationDTO> parseArray(String json) {
        // TODO: разбор JSON-массива уведомлений
        return null;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
