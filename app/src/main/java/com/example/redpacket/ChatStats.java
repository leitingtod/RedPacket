package com.example.redpacket;

import java.util.HashSet;

public class ChatStats {
    private String name = ""; // 聊天名称
    private int total = 0; // 红包总数
    private int opened = 0; // 状态已知的红包个数
    private int unknown = 0; // 状态未知的红包个数
    private int index = 255; // 状态未知的红包当前遍历索引
    private boolean robot = false; // 是否是模拟点击
    public HashSet<Integer> visited = new HashSet<>(); // 保存已访问过的状态未知红包的索引集合

    public void reset() {
        total = 0;
        opened = 0;
        unknown = 255;
        index = 255;
        robot = false;
        visited.clear();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getOpened() {
        return opened;
    }

    public void setOpened(int opened) {
        this.opened = opened;
    }

    public int getUnknown() {
        return unknown;
    }

    public void setUnknown(int unknown) {
        this.unknown = unknown;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isRobot() {
        return robot;
    }

    public void setRobot(boolean robot) {
        this.robot = robot;
    }

    @Override
    public String toString() {
        return "ChatStats{" +
                "name='" + name + '\'' +
                ", total=" + total +
                ", opened=" + opened +
                ", unknown=" + unknown +
                ", index=" + index +
                ", robot=" + robot +
                ", visited=" + visited +
                '}';
    }
}
