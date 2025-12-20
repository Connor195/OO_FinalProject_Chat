package com.chatroom.client.ui;

import com.chatroom.client.model.ChatMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 消息显示面板
 * 用于显示聊天消息，支持 @ 提醒、已读回执、点赞等功能
 */
public class MessagePanel extends JPanel {
    private JTextArea messageArea;
    private SimpleDateFormat timeFormat;
    
    public MessagePanel() {
        timeFormat = new SimpleDateFormat("HH:mm");
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        messageArea.setBackground(Color.WHITE);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = timeFormat.format(new Date(message.getTimestamp()));
            String prefix = message.isGroup() ? "[群聊]" : "[私聊]";
            String from = message.getFromUser();
            String content = message.getContent();
            
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timeStr).append("] ");
            sb.append(prefix).append(" ");
            sb.append(from).append(": ");
            sb.append(content);
            sb.append("\n");
            
            // @ 提醒
            if (message.getAtUsers() != null && !message.getAtUsers().isEmpty()) {
                sb.append("  @ ").append(String.join(", ", message.getAtUsers())).append("\n");
            }
            
            // 反应（点赞/点踩）
            if (message.getReactions() != null && !message.getReactions().isEmpty()) {
                sb.append("  ");
                for (Map.Entry<String, Integer> entry : message.getReactions().entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" ");
                }
                sb.append("\n");
            }
            
            // 已读信息
            if (message.getReadBy() != null && !message.getReadBy().isEmpty()) {
                sb.append("  已读: ").append(String.join(", ", message.getReadBy())).append("\n");
            }
            
            sb.append("\n");
            
            messageArea.append(sb.toString());
            // 自动滚动到底部
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * 添加系统消息
     */
    public void addSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = timeFormat.format(new Date());
            messageArea.append("[" + timeStr + "] [系统] " + message + "\n\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * 添加历史消息列表
     */
    public void addHistoryMessages(List<ChatMessage> messages) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append("========== 历史消息 (" + messages.size() + " 条) ==========\n");
            for (ChatMessage msg : messages) {
                addMessage(msg);
            }
            messageArea.append("==========================================\n\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * 显示消息撤回通知
     */
    public void showMessageRecalled(String msgId, String operator) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = timeFormat.format(new Date());
            messageArea.append("[" + timeStr + "] [系统] " + operator + " 撤回了一条消息\n\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * 显示已读回执
     */
    public void showMessageRead(String msgId, String reader, int readCount) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = timeFormat.format(new Date());
            messageArea.append("[" + timeStr + "] [系统] " + reader + " 已读消息，已读人数: " + readCount + "\n\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * 显示消息反应
     */
    public void showMessageReact(String msgId, String reactType, String operator, boolean isAdd, int count) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = timeFormat.format(new Date());
            String action = isAdd ? "点赞" : "取消点赞";
            messageArea.append("[" + timeStr + "] [系统] " + operator + " " + action + "了消息 (类型: " + reactType + "), 当前总数: " + count + "\n\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * 显示正在输入提示
     */
    public void showTyping(String username) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = timeFormat.format(new Date());
            messageArea.append("[" + timeStr + "] [系统] " + username + " 正在输入...\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * 清空消息
     */
    public void clear() {
        SwingUtilities.invokeLater(() -> {
            messageArea.setText("");
        });
    }
}

