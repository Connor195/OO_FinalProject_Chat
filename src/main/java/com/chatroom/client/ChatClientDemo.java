package com.chatroom.client;

import com.chatroom.client.listener.ChatEventListener;
import com.chatroom.client.model.ChatMessage;
import com.chatroom.client.model.UserInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * 聊天客户端演示程序
 * 提供命令行交互界面
 * 基于 API 文档 v2.0
 */
public class ChatClientDemo {
    private static ChatClient client;
    private static Scanner scanner = new Scanner(System.in);
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static String currentRole = "USER"; // 当前用户角色
    private static List<String> messageIds = new ArrayList<>(); // 存储消息ID用于撤回等操作

    public static void main(String[] args) {
        // 服务器地址，默认 localhost:8080
        String serverUrl = "ws://localhost:8080/chat";
        if (args.length > 0) {
            serverUrl = args[0];
        }

        System.out.println("=== WebSocket 聊天客户端 v2.0 ===");
        System.out.println("服务器地址: " + serverUrl);
        System.out.println();

        // 创建客户端
        client = new ChatClient(serverUrl);

        // 设置事件监听器
        client.setEventListener(new ChatEventListener() {
            @Override
            public void onLoginSuccess(String username, String role, String avatar, long muteEndTime) {
                currentRole = role;
                String roleBadge = "ADMIN".equals(role) ? " [管理员]" : "";
                System.out.println("[" + getCurrentTime() + "] ✓ 登录成功！");
                System.out.println("  用户名: " + username + roleBadge);
                if (muteEndTime > 0) {
                    long currentTime = System.currentTimeMillis();
                    long remaining = (muteEndTime - currentTime) / 1000;
                    System.out.println("  ⚠ 您处于禁言状态，剩余 " + remaining + " 秒");
                }
                System.out.println("输入 'help' 查看可用命令");
            }

            @Override
            public void onChatMessage(ChatMessage message) {
                String timeStr = formatTimestamp(message.getTimestamp());
                String prefix = message.isGroup() ? "[群聊]" : "[私聊]";
                String from = message.getFromUser();
                String content = message.getContent();
                
                // 保存消息ID
                if (message.getMsgId() != null) {
                    messageIds.add(message.getMsgId());
                }

                System.out.println("[" + timeStr + "] " + prefix + " " + from + ": " + content);
                
                // 显示 @ 提醒
                if (message.getAtUsers() != null && !message.getAtUsers().isEmpty()) {
                    System.out.println("  @ " + String.join(", ", message.getAtUsers()));
                }
                
                // 显示反应（点赞/点踩）
                if (message.getReactions() != null && !message.getReactions().isEmpty()) {
                    System.out.println("  反应: " + message.getReactions());
                }
                
                // 显示已读信息
                if (message.getReadBy() != null && !message.getReadBy().isEmpty()) {
                    System.out.println("  已读: " + String.join(", ", message.getReadBy()));
                }
            }

            @Override
            public void onOnlineListUpdate(List<UserInfo> userList) {
                System.out.println("[" + getCurrentTime() + "] 📋 在线用户列表 (" + userList.size() + " 人):");
                for (UserInfo user : userList) {
                    String badge = user.isAdmin() ? " [管理员]" : "";
                    System.out.println("  - " + user.getUsername() + badge);
                }
            }

            @Override
            public void onHistoryList(List<ChatMessage> messages) {
                System.out.println("[" + getCurrentTime() + "] 📜 历史消息 (" + messages.size() + " 条):");
                for (ChatMessage msg : messages) {
                    String timeStr = formatTimestamp(msg.getTimestamp());
                    String prefix = msg.isGroup() ? "[群聊]" : "[私聊]";
                    System.out.println("  [" + timeStr + "] " + prefix + " " + msg.getFromUser() + ": " + msg.getContent());
                }
            }

            @Override
            public void onMessageRecalled(String recalledMsgId, String operator) {
                System.out.println("[" + getCurrentTime() + "] ⚠ " + operator + " 撤回了一条消息 (ID: " + recalledMsgId + ")");
            }

            @Override
            public void onMessageRead(String msgId, String reader, int readCount) {
                System.out.println("[" + getCurrentTime() + "] ✓ " + reader + " 已读消息 (ID: " + msgId + "), 已读人数: " + readCount);
            }

            @Override
            public void onMessageReact(String msgId, String reactType, String operator, boolean isAdd, int count) {
                String action = isAdd ? "点赞" : "取消点赞";
                System.out.println("[" + getCurrentTime() + "] 👍 " + operator + " " + action + "了消息 (类型: " + reactType + "), 当前总数: " + count);
            }

            @Override
            public void onTyping(String username) {
                System.out.println("[" + getCurrentTime() + "] ⌨ " + username + " 正在输入...");
            }

            @Override
            public void onSystemNotice(String text) {
                System.out.println("[" + getCurrentTime() + "] 📢 系统通知: " + text);
            }

            @Override
            public void onError(int code, String message) {
                System.err.println("[" + getCurrentTime() + "] ✗ 错误 [" + code + "]: " + message);
            }

            @Override
            public void onConnectionStateChanged(boolean connected) {
                if (connected) {
                    System.out.println("[" + getCurrentTime() + "] ✓ 已连接到服务器");
                } else {
                    System.out.println("[" + getCurrentTime() + "] ✗ 与服务器断开连接");
                }
            }
        });

        // 连接到服务器
        client.connect();

        // 等待连接建立
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 启动心跳
        client.startHeartbeat();

        // 登录
        System.out.print("请输入用户名: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "User_" + System.currentTimeMillis();
        }
        System.out.print("请输入密码(可选，新用户将自动注册): ");
        String password = scanner.nextLine().trim();
        
        client.login(username, password.isEmpty() ? null : password);

        // 等待登录完成
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 命令循环
        commandLoop();

        // 清理资源
        client.logout();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        client.disconnect();
        scanner.close();
    }

    /**
     * 命令循环
     */
    private static void commandLoop() {
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+", 2);
            String command = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1] : "";

            try {
                switch (command) {
                    case "help":
                        printHelp();
                        break;
                    case "private":
                    case "p":
                        handleSendPrivate(args);
                        break;
                    case "group":
                    case "g":
                        handleSendGroup(args);
                        break;
                    case "recall":
                    case "r":
                        handleRecall(args);
                        break;
                    case "online":
                    case "o":
                        client.getOnlineList();
                        break;
                    case "history":
                    case "h":
                        handleHistory(args);
                        break;
                    case "read":
                        handleRead(args);
                        break;
                    case "typing":
                    case "t":
                        handleTyping(args);
                        break;
                    case "react":
                        handleReact(args);
                        break;
                    case "kick":
                        handleKick(args);
                        break;
                    case "mute":
                        handleMute(args);
                        break;
                    case "quit":
                    case "q":
                    case "exit":
                        return;
                    default:
                        System.out.println("未知命令，输入 'help' 查看帮助");
                }
            } catch (Exception e) {
                System.err.println("执行命令时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 打印帮助信息
     */
    private static void printHelp() {
        System.out.println("\n=== 可用命令 ===");
        System.out.println("help              - 显示此帮助信息");
        System.out.println("private <用户> <消息> 或 p <用户> <消息>  - 发送私聊消息");
        System.out.println("group <群ID> <消息> 或 g <群ID> <消息>  - 发送群聊消息");
        System.out.println("recall <消息ID> 或 r <消息ID>  - 撤回消息（2分钟内）");
        System.out.println("online 或 o  - 获取在线列表");
        System.out.println("history [时间戳] 或 h [时间戳]  - 获取历史消息（不传时间戳获取最新20条）");
        System.out.println("read <消息ID>  - 发送已读回执");
        System.out.println("typing <目标用户/群ID> 或 t <目标用户/群ID>  - 发送正在输入提示");
        System.out.println("react <消息ID> <类型>  - 消息反应（like/dislike/heart等）");
        if ("ADMIN".equals(currentRole)) {
            System.out.println("\n=== 管理员命令 ===");
            System.out.println("kick <用户名>  - 强制踢人");
            System.out.println("mute <用户名> <时长(秒)>  - 禁言用户");
        }
        System.out.println("quit 或 q 或 exit  - 退出程序");
        System.out.println();
    }

    /**
     * 处理发送私聊
     */
    private static void handleSendPrivate(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("用法: private <用户名> <消息内容>");
            System.out.println("示例: private Jerry Hello @Jerry");
            return;
        }
        String targetUser = parts[0];
        String content = parts[1];
        
        // 解析 @ 用户
        List<String> atUsers = extractAtUsers(content);
        
        client.sendPrivateMessage(targetUser, content, atUsers);
        System.out.println("[" + getCurrentTime() + "] 已发送私聊消息给 " + targetUser);
    }

    /**
     * 处理发送群聊
     */
    private static void handleSendGroup(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("用法: group <群ID> <消息内容>");
            System.out.println("示例: group Group1 Hello @Jerry @Tom");
            return;
        }
        String targetGroup = parts[0];
        String content = parts[1];
        
        // 解析 @ 用户
        List<String> atUsers = extractAtUsers(content);
        
        client.sendGroupMessage(targetGroup, content, atUsers);
        System.out.println("[" + getCurrentTime() + "] 已发送群聊消息到 " + targetGroup);
    }

    /**
     * 从消息内容中提取 @ 的用户
     */
    private static List<String> extractAtUsers(String content) {
        List<String> atUsers = new ArrayList<>();
        // 简单的 @ 解析，查找 @用户名 格式
        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word.startsWith("@") && word.length() > 1) {
                atUsers.add(word.substring(1));
            }
        }
        return atUsers;
    }

    /**
     * 处理撤回消息
     */
    private static void handleRecall(String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 1) {
            System.out.println("用法: recall <消息ID>");
            System.out.println("提示: 可以使用最近收到的消息ID");
            if (!messageIds.isEmpty()) {
                System.out.println("最近的消息ID: " + messageIds.get(messageIds.size() - 1));
            }
            return;
        }
        String msgId = parts[0];
        client.recallMessage(msgId);
        System.out.println("[" + getCurrentTime() + "] 正在撤回消息: " + msgId);
    }

    /**
     * 处理获取历史消息
     */
    private static void handleHistory(String args) {
        Long beforeTime = null;
        if (!args.isEmpty()) {
            try {
                beforeTime = Long.parseLong(args.trim());
            } catch (NumberFormatException e) {
                System.out.println("错误: 时间戳格式不正确");
                return;
            }
        }
        client.getHistory(beforeTime);
        System.out.println("[" + getCurrentTime() + "] 正在获取历史消息...");
    }

    /**
     * 处理已读回执
     */
    private static void handleRead(String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 1) {
            System.out.println("用法: read <消息ID>");
            if (!messageIds.isEmpty()) {
                System.out.println("最近的消息ID: " + messageIds.get(messageIds.size() - 1));
            }
            return;
        }
        String msgId = parts[0];
        client.sendMessageRead(msgId);
        System.out.println("[" + getCurrentTime() + "] 已发送已读回执: " + msgId);
    }

    /**
     * 处理正在输入
     */
    private static void handleTyping(String args) {
        if (args.isEmpty()) {
            System.out.println("用法: typing <目标用户/群ID>");
            return;
        }
        client.sendTypingStart(args.trim());
        System.out.println("[" + getCurrentTime() + "] 已发送正在输入提示");
    }

    /**
     * 处理消息反应
     */
    private static void handleReact(String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("用法: react <消息ID> <类型>");
            System.out.println("类型: like, dislike, heart 等");
            return;
        }
        String msgId = parts[0];
        String reactType = parts[1];
        client.sendMessageReact(msgId, reactType);
        System.out.println("[" + getCurrentTime() + "] 已发送反应: " + reactType);
    }

    /**
     * 处理踢人（管理员）
     */
    private static void handleKick(String args) {
        if (!"ADMIN".equals(currentRole)) {
            System.out.println("错误: 您不是管理员，无权限执行此操作");
            return;
        }
        if (args.isEmpty()) {
            System.out.println("用法: kick <用户名>");
            return;
        }
        client.kickUser(args.trim());
        System.out.println("[" + getCurrentTime() + "] 正在踢出用户: " + args.trim());
    }

    /**
     * 处理禁言（管理员）
     */
    private static void handleMute(String args) {
        if (!"ADMIN".equals(currentRole)) {
            System.out.println("错误: 您不是管理员，无权限执行此操作");
            return;
        }
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("用法: mute <用户名> <时长(秒)>");
            return;
        }
        try {
            String targetUser = parts[0];
            int duration = Integer.parseInt(parts[1]);
            client.muteUser(targetUser, duration);
            System.out.println("[" + getCurrentTime() + "] 正在禁言用户: " + targetUser + ", 时长: " + duration + " 秒");
        } catch (NumberFormatException e) {
            System.out.println("错误: 时长必须是数字");
        }
    }

    /**
     * 获取当前时间字符串
     */
    private static String getCurrentTime() {
        return dateFormat.format(new Date());
    }

    /**
     * 格式化时间戳
     */
    private static String formatTimestamp(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }
}
