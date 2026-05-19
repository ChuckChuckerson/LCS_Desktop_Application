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
        if (json == null || json.isBlank()) return List.of();
        List<NotificationDTO> out = new ArrayList<>();
        Pattern obj = Pattern.compile("\\{([^}]*)\\}");
        Matcher m = obj.matcher(json);
        while (m.find()) {
            String body = m.group(1);
            NotificationDTO n = new NotificationDTO();
            n.id = uuid(get(body, "id"));
            n.type = unescape(get(body, "type"));
            String createdAt = get(body, "createdAt");
            n.createdAt = createdAt != null && !createdAt.isBlank() ? OffsetDateTime.parse(createdAt) : null;
            n.caseId = uuid(get(body, "caseId"));
            n.message = unescape(get(body, "message"));
            out.add(n);
        }
        return out;
    }
        
    private static UUID uuid(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    private static String get(String body, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])+)\"");
        Matcher m = p.matcher(body);
        if (!m.find()) return null;
        return m.group(1);
    }

    private static String unescape(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
