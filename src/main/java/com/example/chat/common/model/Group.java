package com.example.chat.common.model;

import lombok.Data;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class Group {
    private String groupId;
    private String groupName;
    private String owner;
    // 线程安全的 List，适合读多写少
    private List<String> members = new CopyOnWriteArrayList<>();
    private List<String> admins = new CopyOnWriteArrayList<>();
}