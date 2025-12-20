package com.chatroom.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 登录窗口
 * 基于 API 文档 v2.0
 */
public class LoginWindow extends JFrame {
    private JTextField serverUrlField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    
    private LoginCallback callback;
    
    public interface LoginCallback {
        void onLogin(String serverUrl, String username, String password);
    }
    
    public LoginWindow(LoginCallback callback) {
        this.callback = callback;
        initComponents();
    }
    
    private void initComponents() {
        setTitle("聊天室客户端 - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // 标题
        JLabel titleLabel = new JLabel("WebSocket 聊天室客户端", JLabel.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 服务器地址
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("服务器地址:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        serverUrlField = new JTextField("ws://localhost:8080/chat", 20);
        formPanel.add(serverUrlField, gbc);
        
        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);
        
        // 密码
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("密码(可选):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // 状态标签
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.GRAY);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        loginButton = new JButton("登录");
        loginButton.setPreferredSize(new Dimension(120, 35));
        loginButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        buttonPanel.add(loginButton);
        
        // 添加按钮面板到表单下方
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);
        
        add(mainPanel);
        
        // 事件处理
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        // 回车键登录
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        
        usernameField.addKeyListener(enterKeyAdapter);
        passwordField.addKeyListener(enterKeyAdapter);
        serverUrlField.addKeyListener(enterKeyAdapter);
        
        pack();
    }
    
    private void performLogin() {
        String serverUrl = serverUrlField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        
        if (serverUrl.isEmpty()) {
            showStatus("请输入服务器地址", Color.RED);
            return;
        }
        
        if (username.isEmpty()) {
            showStatus("请输入用户名", Color.RED);
            return;
        }
        
        // 如果密码为空，传 null
        String passwordToSend = password.isEmpty() ? null : password;
        
        loginButton.setEnabled(false);
        showStatus("正在连接...", Color.BLUE);
        
        if (callback != null) {
            callback.onLogin(serverUrl, username, passwordToSend);
        }
    }
    
    public void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
    
    public void reset() {
        loginButton.setEnabled(true);
        statusLabel.setText(" ");
        statusLabel.setForeground(Color.GRAY);
    }
}

