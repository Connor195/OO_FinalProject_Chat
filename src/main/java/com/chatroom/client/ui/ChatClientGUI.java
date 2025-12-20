package com.chatroom.client.ui;

import com.chatroom.client.ChatClient;
import com.chatroom.client.listener.ChatEventListener;
import com.chatroom.client.model.ChatMessage;
import com.chatroom.client.model.UserInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天客户端 GUI 主窗口
 * 基于 API 文档 v2.0 实现
 */
public class ChatClientGUI extends JFrame {
    private ChatClient client;
    private String currentUsername;
    private String currentRole = "USER";
    private long muteEndTime = 0;
    
    // UI 组件
    private LoginWindow loginWindow;
    private MessagePanel messagePanel;
    private JTextArea inputArea;
    private JComboBox<String> targetComboBox;
    private JComboBox<String> chatTypeComboBox;
    private JList<String> onlineUserList;
    private DefaultListModel<String> onlineUserListModel;
    private JButton sendButton;
    private JButton recallButton;
    private JButton reactButton;
    private JButton historyButton;
    private JButton onlineButton;
    private JButton kickButton;
    private JButton muteButton;
    private JLabel statusLabel;
    private JLabel roleLabel;
    private JLabel typingLabel;
    
    private List<String> recentMessageIds = new ArrayList<>();
    
    public ChatClientGUI() {
        initComponents();
        showLoginWindow();
    }
    
    private void initComponents() {
        setTitle("聊天室客户端");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 顶部状态栏
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.NORTH);
        
        // 中间内容区域
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // 左侧：消息显示区域
        messagePanel = new MessagePanel();
        JScrollPane messageScrollPane = new JScrollPane(messagePanel);
        messageScrollPane.setBorder(new TitledBorder("消息区域"));
        contentPanel.add(messageScrollPane, BorderLayout.CENTER);
        
        // 右侧：在线用户列表
        JPanel rightPanel = createRightPanel();
        contentPanel.add(rightPanel, BorderLayout.EAST);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // 底部：输入区域
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * 创建状态栏
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.setBackground(new Color(240, 240, 240));
        
        statusLabel = new JLabel("未连接");
        statusLabel.setForeground(Color.GRAY);
        panel.add(statusLabel);
        
        panel.add(Box.createHorizontalStrut(20));
        
        roleLabel = new JLabel("");
        roleLabel.setForeground(new Color(255, 140, 0));
        panel.add(roleLabel);
        
        panel.add(Box.createHorizontalStrut(20));
        
        typingLabel = new JLabel("");
        typingLabel.setForeground(Color.BLUE);
        typingLabel.setFont(new Font("Microsoft YaHei", Font.ITALIC, 12));
        panel.add(typingLabel);
        
        return panel;
    }
    
    /**
     * 创建右侧面板（在线用户列表）
     */
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(200, 0));
        panel.setBorder(new TitledBorder("在线用户"));
        
        onlineUserListModel = new DefaultListModel<>();
        onlineUserList = new JList<>(onlineUserListModel);
        onlineUserList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineUserList.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(onlineUserList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 刷新按钮
        onlineButton = new JButton("刷新列表");
        onlineButton.addActionListener(e -> {
            if (client != null) {
                client.getOnlineList();
            }
        });
        panel.add(onlineButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建输入面板
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 顶部：目标选择和聊天类型
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JLabel typeLabel = new JLabel("类型:");
        topPanel.add(typeLabel);
        chatTypeComboBox = new JComboBox<>(new String[]{"私聊", "群聊"});
        topPanel.add(chatTypeComboBox);
        
        topPanel.add(Box.createHorizontalStrut(10));
        
        JLabel targetLabel = new JLabel("目标:");
        topPanel.add(targetLabel);
        targetComboBox = new JComboBox<>();
        targetComboBox.setEditable(true);
        targetComboBox.setPreferredSize(new Dimension(150, 25));
        topPanel.add(targetComboBox);
        
        panel.add(topPanel, BorderLayout.NORTH);
        
        // 中间：输入框
        inputArea = new JTextArea(3, 50);
        inputArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createLoweredBevelBorder());
        
        // 回车发送（Ctrl+Enter 换行）
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isControlDown()) {
                        // Ctrl+Enter 换行
                        return;
                    } else {
                        // Enter 发送
                        e.consume();
                        sendMessage();
                    }
                }
            }
        });
        
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        panel.add(inputScrollPane, BorderLayout.CENTER);
        
        // 底部：按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);
        
        recallButton = new JButton("撤回");
        recallButton.addActionListener(e -> recallMessage());
        buttonPanel.add(recallButton);
        
        reactButton = new JButton("点赞");
        reactButton.addActionListener(e -> reactMessage());
        buttonPanel.add(reactButton);
        
        historyButton = new JButton("历史");
        historyButton.addActionListener(e -> loadHistory());
        buttonPanel.add(historyButton);
        
        // 管理员按钮
        kickButton = new JButton("踢人");
        kickButton.addActionListener(e -> kickUser());
        kickButton.setEnabled(false);
        buttonPanel.add(kickButton);
        
        muteButton = new JButton("禁言");
        muteButton.addActionListener(e -> muteUser());
        muteButton.setEnabled(false);
        buttonPanel.add(muteButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 显示登录窗口
     */
    private void showLoginWindow() {
        loginWindow = new LoginWindow((serverUrl, username, password) -> {
            loginWindow.showStatus("正在连接...", Color.BLUE);
            connectAndLogin(serverUrl, username, password);
        });
        loginWindow.setVisible(true);
    }
    
    /**
     * 连接并登录
     */
    private void connectAndLogin(String serverUrl, String username, String password) {
        try {
            client = new ChatClient(serverUrl);
            currentUsername = username;
            
            // 设置事件监听器
            client.setEventListener(new ChatEventListener() {
                @Override
                public void onLoginSuccess(String username, String role, String avatar, long muteEndTime) {
                    SwingUtilities.invokeLater(() -> {
                        currentRole = role;
                        ChatClientGUI.this.muteEndTime = muteEndTime;
                        
                        // 更新 UI
                        statusLabel.setText("已连接 - " + username);
                        statusLabel.setForeground(Color.GREEN);
                        
                        if ("ADMIN".equals(role)) {
                            roleLabel.setText("【管理员】");
                            roleLabel.setForeground(new Color(255, 215, 0));
                            kickButton.setEnabled(true);
                            muteButton.setEnabled(true);
                        } else {
                            roleLabel.setText("");
                            kickButton.setEnabled(false);
                            muteButton.setEnabled(false);
                        }
                        
                        if (muteEndTime > 0) {
                            long remaining = (muteEndTime - System.currentTimeMillis()) / 1000;
                            if (remaining > 0) {
                                statusLabel.setText(statusLabel.getText() + " (禁言中，剩余 " + remaining + " 秒)");
                            }
                        }
                        
                        // 关闭登录窗口，显示主窗口
                        loginWindow.setVisible(false);
                        setVisible(true);
                        
                        // 获取在线列表
                        client.getOnlineList();
                    });
                }
                
                @Override
                public void onChatMessage(ChatMessage message) {
                    SwingUtilities.invokeLater(() -> {
                        messagePanel.addMessage(message);
                        if (message.getMsgId() != null) {
                            recentMessageIds.add(message.getMsgId());
                            // 只保留最近 50 条消息 ID
                            if (recentMessageIds.size() > 50) {
                                recentMessageIds.remove(0);
                            }
                        }
                    });
                }
                
                @Override
                public void onOnlineListUpdate(List<UserInfo> userList) {
                    SwingUtilities.invokeLater(() -> {
                        onlineUserListModel.clear();
                        for (UserInfo user : userList) {
                            String display = user.getUsername();
                            if (user.isAdmin()) {
                                display += " [管理员]";
                            }
                            onlineUserListModel.addElement(display);
                            
                            // 更新目标下拉框
                            if (!user.getUsername().equals(currentUsername)) {
                                boolean exists = false;
                                for (int i = 0; i < targetComboBox.getItemCount(); i++) {
                                    if (targetComboBox.getItemAt(i).equals(user.getUsername())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    targetComboBox.addItem(user.getUsername());
                                }
                            }
                        }
                    });
                }
                
                @Override
                public void onHistoryList(List<ChatMessage> messages) {
                    SwingUtilities.invokeLater(() -> {
                        messagePanel.addHistoryMessages(messages);
                    });
                }
                
                @Override
                public void onMessageRecalled(String recalledMsgId, String operator) {
                    SwingUtilities.invokeLater(() -> {
                        messagePanel.showMessageRecalled(recalledMsgId, operator);
                        recentMessageIds.remove(recalledMsgId);
                    });
                }
                
                @Override
                public void onMessageRead(String msgId, String reader, int readCount) {
                    SwingUtilities.invokeLater(() -> {
                        messagePanel.showMessageRead(msgId, reader, readCount);
                    });
                }
                
                @Override
                public void onMessageReact(String msgId, String reactType, String operator, boolean isAdd, int count) {
                    SwingUtilities.invokeLater(() -> {
                        messagePanel.showMessageReact(msgId, reactType, operator, isAdd, count);
                    });
                }
                
                @Override
                public void onTyping(String username) {
                    SwingUtilities.invokeLater(() -> {
                        typingLabel.setText(username + " 正在输入...");
                        // 3 秒后清除
                        Timer timer = new Timer(3000, e -> typingLabel.setText(""));
                        timer.setRepeats(false);
                        timer.start();
                    });
                }
                
                @Override
                public void onSystemNotice(String text) {
                    SwingUtilities.invokeLater(() -> {
                        messagePanel.addSystemMessage(text);
                    });
                }
                
                @Override
                public void onError(int code, String message) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ChatClientGUI.this, 
                            "错误 [" + code + "]: " + message, 
                            "错误", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
                
                @Override
                public void onConnectionStateChanged(boolean connected) {
                    SwingUtilities.invokeLater(() -> {
                        if (connected) {
                            statusLabel.setText("已连接");
                            statusLabel.setForeground(Color.GREEN);
                        } else {
                            statusLabel.setText("连接断开");
                            statusLabel.setForeground(Color.RED);
                        }
                    });
                }
            });
            
            // 连接
            client.connect();
            
            // 等待连接建立后登录
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    client.login(username, password);
                    client.startHeartbeat();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (Exception e) {
            loginWindow.showStatus("连接失败: " + e.getMessage(), Color.RED);
            loginWindow.reset();
            e.printStackTrace();
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessage() {
        if (client == null || !client.isConnected()) {
            JOptionPane.showMessageDialog(this, "未连接到服务器", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String content = inputArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }
        
        String target = (String) targetComboBox.getSelectedItem();
        if (target == null || target.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择或输入目标用户/群ID", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查禁言状态
        if (muteEndTime > 0 && System.currentTimeMillis() < muteEndTime) {
            long remaining = (muteEndTime - System.currentTimeMillis()) / 1000;
            JOptionPane.showMessageDialog(this, "您处于禁言状态，剩余 " + remaining + " 秒", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 解析 @ 用户
        List<String> atUsers = extractAtUsers(content);
        
        // 发送消息
        String chatType = (String) chatTypeComboBox.getSelectedItem();
        if ("私聊".equals(chatType)) {
            client.sendPrivateMessage(target.trim(), content, atUsers);
        } else {
            client.sendGroupMessage(target.trim(), content, atUsers);
        }
        
        // 发送正在输入提示（可选）
        // client.sendTypingStart(target.trim());
        
        // 清空输入框
        inputArea.setText("");
    }
    
    /**
     * 从消息内容中提取 @ 的用户
     */
    private List<String> extractAtUsers(String content) {
        List<String> atUsers = new ArrayList<>();
        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word.startsWith("@") && word.length() > 1) {
                atUsers.add(word.substring(1));
            }
        }
        return atUsers;
    }
    
    /**
     * 撤回消息
     */
    private void recallMessage() {
        if (recentMessageIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可撤回的消息", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String msgId = recentMessageIds.get(recentMessageIds.size() - 1);
        client.recallMessage(msgId);
        messagePanel.addSystemMessage("正在撤回消息: " + msgId);
    }
    
    /**
     * 消息反应（点赞）
     */
    private void reactMessage() {
        if (recentMessageIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可操作的消息", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String msgId = recentMessageIds.get(recentMessageIds.size() - 1);
        client.sendMessageReact(msgId, "like");
        messagePanel.addSystemMessage("已点赞消息: " + msgId);
    }
    
    /**
     * 加载历史消息
     */
    private void loadHistory() {
        String input = JOptionPane.showInputDialog(this, 
            "输入时间戳（毫秒）获取该时间之前的消息，留空获取最新20条:", 
            "获取历史消息");
        
        Long beforeTime = null;
        if (input != null && !input.trim().isEmpty()) {
            try {
                beforeTime = Long.parseLong(input.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "时间戳格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        client.getHistory(beforeTime);
    }
    
    /**
     * 踢人（管理员功能）
     */
    private void kickUser() {
        if (!"ADMIN".equals(currentRole)) {
            JOptionPane.showMessageDialog(this, "您不是管理员，无权限执行此操作", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String selected = onlineUserList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请选择要踢出的用户", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 提取用户名（去掉 [管理员] 标识）
        String username = selected.split(" ")[0];
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要踢出用户 " + username + " 吗？", 
            "确认", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            client.kickUser(username);
            messagePanel.addSystemMessage("正在踢出用户: " + username);
        }
    }
    
    /**
     * 禁言用户（管理员功能）
     */
    private void muteUser() {
        if (!"ADMIN".equals(currentRole)) {
            JOptionPane.showMessageDialog(this, "您不是管理员，无权限执行此操作", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String selected = onlineUserList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请选择要禁言的用户", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 提取用户名
        String username = selected.split(" ")[0];
        
        String durationStr = JOptionPane.showInputDialog(this, 
            "请输入禁言时长（秒）:", 
            "禁言用户");
        
        if (durationStr != null && !durationStr.trim().isEmpty()) {
            try {
                int duration = Integer.parseInt(durationStr.trim());
                client.muteUser(username, duration);
                messagePanel.addSystemMessage("正在禁言用户: " + username + ", 时长: " + duration + " 秒");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "时长必须是数字", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new ChatClientGUI();
        });
    }
}

