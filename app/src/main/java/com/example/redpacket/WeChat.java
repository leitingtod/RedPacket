package com.example.redpacket;

import android.app.Notification;
import android.app.PendingIntent;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.orhanobut.logger.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WeChat {
    private static final String ChatListPage = "ChatList";
    private static final String ChatPage = "Chat";
    private static final String RedPacketGrabPage = "RedPacketGrab";
    private static final String RedPacketDetailPage = "RedPacketDetail";
    private static final String UnknownPage = "Unknown";

    private static boolean mutex = false; // 在一个事件未处理完时，拒绝新的事件
    private static String currPage = ""; // 记录当前的页面类型
    private static String handledPage = ""; // 记录上次搜索过的页面类型

    public static ChatStats chatStats = new ChatStats();
    public static boolean scrollStart = false;
    public static boolean scrollStop = false;
    public static boolean eventStart = false;
    public static boolean eventStop = false;
    public static boolean windowStateChange = false;

    //=======================================================
    // 事件处理函数
    //=======================================================

    public static void process(MonitorService service, AccessibilityEvent event) {
        eventStart = true;

        int et = event.getEventType();
        String etName = AccessibilityEvent.eventTypeToString(et);
        String message = "处理事件 " + etName;

        if (!mutex) {
            mutex = true;
            Logger.v("开始%s : %s", message, getStates());

        } else {
            Logger.v("正在处理事件，忽略事件 %s : %s", etName, getStates());
            return;
        }

        switch (et) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotification(service, event);
                break;

            // 在一个应用与桌面间来回切换，有时并不触发此事件，导致有时并不搜索红包
            // 且返回桌面不产生任何事件
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                windowStateChange = true;
                String className = event.getClassName().toString();
                AccessibilityNodeInfo root = service.getRootInActiveWindow();

                if (isChatListPage(root, className)) {
                    setCurrentPage(ChatListPage);
                } else if (isChatPage(root, className)) {
                    setCurrentPage(ChatPage);
                } else if (isRedPacketGrabPage(className)) {
                    setCurrentPage(RedPacketGrabPage);
                } else if (isRedPacketDetailPage(className)) {
                    setCurrentPage(RedPacketDetailPage);
                } else {
                    setCurrentPage(UnknownPage);
                }
                break;

            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                AccessibilityNodeInfo root1 = service.getRootInActiveWindow();

                if (isChatListPage(root1)) {
                    setCurrentPage(ChatListPage);
                } else if (isChatPage(root1)) {
                    setCurrentPage(ChatPage);
                } else {
                    // RedPacketGrabPage, RedPacketDetailPage 两个页面会因 WINDOW_CONTENT_CHANGED
                    // 事件导致判断错误，使当前页面的类型为Unknown，需要排除这两个页面
                    setCurrentPageUnknown();
                }
                break;

            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                scrollStart = true;
                Logger.d("页面开始/正在滚动: %s", message, getStates());
                break;

            default:
                Logger.v("不%s : %s", message, getStates());
                message = "";
                break;
        }
        mutex = false;
    }

    public static void handleEventStop(MonitorService service) {
        Logger.w("EventType: TYPE_EVENT_STOP\n" +
                "ClassName: com.example.redpacket.MonitorService, " +
                "Package: com.example.redpacket");

        String message = "处理事件 TYPE_EVENT_STOP";
        Logger.v("开始%s : %s", message, getStates());

        if (scrollStop) {
            Logger.d("当面滚动的页面为 %s", getCurrPage());
        }

        dispatch(service);
        scrollStop = false;
        eventStop = false;

        Logger.v("结束%s : %s", message, getStates());
    }

    // todo 好像只能处理一个红包消息，若是同时来多个如何处理？但已经够用了，忽略。
    private static void handleNotification(MonitorService service, AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        for (CharSequence text : texts) {
            String content = text.toString();
            if (!TextUtils.isEmpty(content)) {
                if (content.contains(WeChatConfig.RedPacketKey)) {
                    Logger.d("发现微信红包通知");
                    if (!Utils.isScreenOn(service)) {
                        Utils.wakeUpScreen(service);
                    }
                    gotoChatPage(event);
                    resetChatStats();
                } else if (content.contains(WeChatConfig.UnNormalRedPacketKey)) {
                    Logger.d("发现红包异常");
                }
            }
        }
    }

    //=======================================================
    // 抢红包处理函数
    //=======================================================

    private static void dispatch(MonitorService service) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();

        switch (getCurrPage()) {
            case ChatListPage:
                handleChatListPage(service, root);
                setHandledPage(ChatListPage);
                break;
            case ChatPage:
                handleChatPage(service, root);
                setHandledPage(ChatPage);
                break;
            case RedPacketGrabPage:
                handleRedPacketGrabPage(root);
                setHandledPage(RedPacketGrabPage);
                break;
            case RedPacketDetailPage:
                handleRedPacketDetailPage(root);
                setHandledPage(RedPacketDetailPage);
                break;
            default:
                Logger.d("当前页面为 %s，不在需要处理的页面中，不执行任何操作", getCurrPage());
                setHandledPage(UnknownPage);
                break;
        }
        windowStateChange = false;
    }

    private static void handleChatListPage(MonitorService service, AccessibilityNodeInfo
            root) {
        Logger.d("当前页面为 %s，检查页面是否转换", getCurrPage());

        if (!(isPageChanged() || scrollStop) && !windowStateChange) {
            if (scrollStart) {
                Logger.d("未发现页面转换，但页面正在滚动，等待滚动停止");
            } else {
                Logger.d("未发现页面转换或滚动，不执行任何操作");
            }
            return;
        } else {
            if (isPageChanged()) {
                Logger.d("发现页面转换，开始搜索哪一个聊天会话有红包");
            }
            if (scrollStop) {
                Logger.d("发现页面滚动停止，开始搜索哪一个聊天会话有红包");
            }
        }

        // 获取第一个找到的含有[微信红包]关键字且未读消息个数大于0的聊天
        AccessibilityNodeInfo chat = Utils.findNodeInfosById(root, WeChatConfig.ChatListPage_Item_ID)
                .orElse(Collections.emptyList())
                .stream()
                .filter(item -> {
                    boolean hasRedPacket = Utils.findNodeInfoById(item, WeChatConfig.ChatListPage_Item_Content_ID)
                            .filter(Objects::nonNull)
                            .filter(node -> {
                                String s = Optional.ofNullable(node.getText())
                                        .orElse("").toString();
                                Logger.d("此聊天的文字标识：%s, 红包: %s",
                                        s, s.contains(WeChatConfig.RedPacketKey));
                                return s.contains(WeChatConfig.RedPacketKey);
                            }).isPresent();


                    boolean hasNewMsg = Utils.findNodeInfoById(item, WeChatConfig.ChatListPage_Item_MsgCount_ID)
                            .filter(Objects::nonNull)
                            .filter(node -> {
                                int count = Integer.parseInt(Optional.ofNullable(node.getText())
                                        .orElse("0").toString());
                                Logger.d("此聊天的未读消息个数：%s", count);
                                return count > 0;
                            }).isPresent();

                    Logger.d("** %s, %s ",
                            hasRedPacket ? "此聊天有红包，" : "此聊天没有红包，",
                            hasNewMsg ? "有新的消息" : "无新的消息");

                    return hasRedPacket && hasNewMsg;
                }).findFirst().orElse(null);

        if (chat == null) {
            Logger.d("未发现哪一个聊天会话有新的红包，不执行任何操作");
            service.toast(R.string.redpacket_over);
        } else {
            Logger.d("发现一个聊天会话有新的红包, 进入搜索");
            Utils.findNodeInfoById(chat, WeChatConfig.ChatListPage_Item_Content_ID).ifPresent(node -> {
                openChatPage(node);
            });
        }
    }

    private static void handleChatPage(MonitorService service, AccessibilityNodeInfo root) {
        Logger.d("当前页面为 %s，检查页面是否转换", getCurrPage());

        if (!(isPageChanged() || scrollStop) && !windowStateChange) {
            if (scrollStart) {
                Logger.d("未发现页面转换，但页面正在滚动，等待滚动停止");
            } else {
                Logger.d("未发现页面转换或滚动，不执行任何操作");
            }
            return;
        } else {
            if (isPageChanged()) {
                Logger.d("发现页面转换，%s -> %s, 开始搜索哪些红包可以抢",
                        getHandledPage(), getCurrPage());

                // 从 RedPacketGrabPage, RedPacketDetailPage 这两个页面转换到聊天会话页面，不能重置内部状态。
                // 这三个页面的互相跳转组成一个循环抢红包的过程
                if (Arrays.asList(RedPacketGrabPage, RedPacketDetailPage).contains(getHandledPage())) {
                    Logger.d("从 %s 页面转换过来，不能重置内部状态", getHandledPage());
                } else {
                    Logger.d("从 %s 页面转换过来，不在 %s, %s 两个页面中，重置内部状态",
                            getHandledPage(), RedPacketGrabPage, RedPacketDetailPage);
                    resetChatStats();
                }
            }

            if (scrollStop) {
                Logger.d("发现页面滚动停止，开始搜索哪些红包可以抢");
                resetChatStats();
            }
        }

        String name = getChatName(root);

        if (name.isEmpty()) {
            Logger.d("聊天会话页面名称为空，放弃这次查找" + name);
            return;
        }

        // 当聊天名称改变时，简单处理，认为需要重新统计，清空之前聊天会话的统计信息
        // 因此能处理单聊双开的问题，前提是名称不同。
        // 对于群聊问题，名称是一样的，因此双开情景无法处理。
        // 对于单开聊天名称一样的情况不作考虑
        if (isChatNameChanged(name)) {
            Logger.d("发现聊天名称改变：" + chatStats.getName() + " -> " + name);
            resetChatStats();
        }

        Logger.d("当前聊天会话页面名称：" + name);

        chatStats.setName(name);

        // 获取所有红包
        List<AccessibilityNodeInfo> redPacketList = Utils
                .findNodeInfosById(root, WeChatConfig.ChatPage_RedPacket_Id)
                .orElse(Collections.emptyList());

        chatStats.setTotal(redPacketList.size());

        // 获取所有已领取过的红包
        List<AccessibilityNodeInfo> openedRedPacketList = redPacketList
                .stream().filter(WeChat::isRedPacketOpened).collect(Collectors.toList());

        chatStats.setOpened(openedRedPacketList.size());

        // 获取所有状态未知的红包(即使领取过，有些红包的状态也不更新: 如自己发的未被抢光的红包，异常的红包)
        List<AccessibilityNodeInfo> unknownRedPacketList = redPacketList
                .stream().filter(node -> {
                    return !isRedPacketOpened(node);
                }).collect(Collectors.toList());

        // 计算未知状态红包个数的变化(仅在同一个聊天会话页面有效工作)
        // 对于双开微信，虽然群聊的名称一样，但实际是属于两个不同的聊天会话页面
        int delta = chatStats.getUnknown() - unknownRedPacketList.size();
        Logger.d("计算未知状态红包个数变化: dalta(" + delta + ") = lastUnknown(" +
                chatStats.getUnknown() + ") - unknown(" + unknownRedPacketList.size() + ")");

        chatStats.setUnknown(unknownRedPacketList.size());

        if (unknownRedPacketList.isEmpty()) {
            service.toast(R.string.redpacket_over);
            Logger.d("已领取所有红包(未打开的红包个数为0)");
        } else {
            // 从最后一个红包向上逐个打开
            int index = chatStats.getIndex();

            if (index == 255) {
                index = unknownRedPacketList.size() - 1;
            }

            Logger.d("准备红包索引 index = " + index);

            // 未知状态红包的个数 >= 0, 不论红包状态是否更新，都已访问过，索引不变
            // 若是<0, 则说明有新的未知状态红包，立即修改索引，跳跃访问
            // 对于已经访问过的，增加集合状态过滤
            if (delta < 0) {
                index = unknownRedPacketList.size() - 1;
                Logger.d("发现状态未知的红包个数变化 delta(" +
                        delta + ") < 0, 更新 index = " + index);
            }

            // 跳过已领取过的状态未知的红包，此类情况应很少出现
            while (true) {
                if (chatStats.visited.contains(index)) {
                    Logger.d("此状态未知的红包[" + index + "]已领取过");
                    index--;
                } else {
                    break;
                }
            }

            if (index < 0) {
                service.toast(R.string.redpacket_over);
                Logger.d("已领取所有红包(已遍历完所有状态未知的红包)");
            } else {
                Logger.d("红包[" + index + "]: 准备打开");
                openRedPacketGrabPage(unknownRedPacketList.get(index));
                chatStats.setIndex(index - 1);
                chatStats.setRobot(true);
                chatStats.visited.add(index);
            }
        }
    }

    private static void handleRedPacketGrabPage(AccessibilityNodeInfo root) {
        Logger.d("当前页面为 %s，检查红包状态, 无需检查页面是否转换", getCurrPage());

        if (isRedPacketOver(root)) {
            Logger.d("手慢了，红包派完了");

            if (isRobotClicked()) {
                Logger.d("模拟点击，准备自动关闭红包详情页面");
                chatStats.setRobot(false);
                closeRedPacketGrabPage(root);
            } else {
                Logger.d("用户点击，不关闭红包详情页面");
            }
        } else {
            Logger.d("立即抢红包");
            grabRedPacket(root);
        }
    }

    private static void handleRedPacketDetailPage(AccessibilityNodeInfo root) {
        Logger.d("当前页面为 %s，无需检查页面是否转换", getCurrPage());

        if (isRobotClicked()) {
            Logger.d("模拟点击，准备自动关闭红包详情页面");
            chatStats.setRobot(false);
            closeRedPacketDetailPage(root);
        } else {
            Logger.d("用户点击，不关闭红包详情页面");
        }
    }

    //=======================================================
    // 判断页面类型
    //=======================================================

    private static boolean isChatListPage(AccessibilityNodeInfo root, String className) {
        if (className.equals(WeChatConfig.ChatListPage_ClassName)) {
            return isChatListPage(root);
        }
        return false;
    }

    private static boolean isChatListPage(AccessibilityNodeInfo root) {
        // 根据聊天列表页面左上角的名称判断

        return Utils.findNodeInfoById(root, WeChatConfig.ChatListPage_Name_ID)
                .filter(Objects::nonNull)
                .filter(node -> {
                    String content = Optional.ofNullable(node.getText())
                            .orElse("").toString();
                    return content.contains(WeChatConfig.ChatListPage_Name);
                }).isPresent();
    }

    private static boolean isChatPage(AccessibilityNodeInfo root, String className) {
        if (className.equals(WeChatConfig.ChatListPage_ClassName)
                || className.equals(WeChatConfig.ChatPage_ClassName1)) {
            return (isChatPage(root));
        }
        return false;
    }

    private static boolean isChatPage(AccessibilityNodeInfo root) {
        // 根据聊天会话页面的返回按钮ID判断
        return Utils.findNodeInfoById(root, WeChatConfig.ChatPage_Back_ID).isPresent();
    }

    private static boolean isRedPacketGrabPage(String className) {
        return className.equals(WeChatConfig.RedPacketGrabPage_ClassName);
    }

    private static boolean isRedPacketDetailPage(String className) {
        return className.equals(WeChatConfig.RedPacketDetailPage_ClassName);
    }

    private static boolean isRedPacketOver(AccessibilityNodeInfo root) {
        return Utils.findNodeInfoById(root, WeChatConfig.RedPacketGrabPage_Content_ID)
                .filter(Objects::nonNull)
                .filter(node -> {
                    String content = Optional.ofNullable(node.getText())
                            .orElse("").toString();
                    return content.contains(WeChatConfig.RedPacketGrabPage_Content_RedPacketOver) ||
                            content.contains(WeChatConfig.RedPacketGrabPage_Content_RedPacketExpired);
                }).isPresent();
    }

    private static String getChatName(AccessibilityNodeInfo root) {
        return Utils.findNodeInfoById(root, WeChatConfig.ChatPage_Name_ID)
                .filter(Objects::nonNull)
                .map(node -> {
                    return Optional.ofNullable(node.getText())
                            .orElse("").toString();
                }).orElse("");
    }

    private static int getChatStaffNumber(String chatName) {
        int count = 1;

        Pattern pen = Pattern.compile("\\(([^)]*)\\)");
        Matcher men = pen.matcher(chatName);

        // 获取群聊聊天详情页面标题中显示的人数
        if (men.find()) {
            Logger.d(men.group());
            count = Integer.parseInt(men.group(1));
        } else {
            Pattern pzh = Pattern.compile("\\（([^）]*)\\）");
            Matcher mzh = pzh.matcher(chatName);
            if (mzh.find()) {
                Logger.d(mzh.group());
                count = Integer.parseInt(men.group(1));
            }
        }
        return count;
    }

    //=======================================================
    // 模拟用户识别、点击操作
    //=======================================================

    private static boolean isPageChanged() {
        return !getCurrPage().equals(getHandledPage());
    }

    private static boolean isChatNameChanged(String name) {
        return !chatStats.getName().equals(name);
    }

    private static boolean isRedPacketOpened(AccessibilityNodeInfo root) {
        return Utils.findNodeInfoById(root, WeChatConfig.ChatPage_RedPacket_State_ID).isPresent();
    }

    private static void gotoChatPage(AccessibilityEvent event) {
        //A instanceof B 用来判断内存中实际对象A是不是B类型，常用于强制转换前的判断
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Logger.d("发现微信红包通知，打开对应的聊天会话页面");
            Notification notification = (Notification) event.getParcelableData();
            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    private static void openChatPage(AccessibilityNodeInfo root) {
        Logger.d("点击聊天列表条目，打开对应的聊天会话页面");
        Optional.ofNullable(root).ifPresent(node -> {
            Utils.performClick(node, MonitorService.loadingTime);
        });
    }

    private static void closeChatPage(AccessibilityNodeInfo root) {
        Logger.d("点击返回按钮，关闭聊天会话页面");
        Utils.findNodeInfoById(root, WeChatConfig.ChatPage_Back_ID).ifPresent(node -> {
            Utils.performClick(node, MonitorService.loadingTime);
        });
    }

    private static void grabRedPacket(AccessibilityNodeInfo root) {
        Logger.d("点击抢红包按钮");
        Utils.findNodeInfoById(root, WeChatConfig.RedPacketGrabPage_Grab_ID).ifPresent(node -> {
            Utils.performClick(node, MonitorService.openDelayTime);
        });
    }

    private static void openRedPacketGrabPage(AccessibilityNodeInfo root) {
        Logger.d("点击红包，打开抢红包页面");
        Utils.findNodeInfoById(root, WeChatConfig.ChatPage_RedPacket_Open_ID).ifPresent((node) -> {
            Utils.performClick(node, MonitorService.loadingTime);
            Logger.i(chatStats.toString());
        });
    }

    private static void closeRedPacketGrabPage(AccessibilityNodeInfo root) {
        Logger.d("点击关闭按钮，关闭抢红包页面");
        Utils.findNodeInfoById(root, WeChatConfig.RedPacketGrabPage_Back_ID).ifPresent(node -> {
            Utils.performClick(node, MonitorService.loadingTime);
        });
    }

    private static void closeRedPacketDetailPage(AccessibilityNodeInfo root) {
        Logger.d("点击返回按钮，关闭红包详情页面");
        Utils.findNodeInfoById(root, WeChatConfig.RedPacketDetailPage_Back_ID).ifPresent(node -> {
            Utils.performClick(node, MonitorService.loadingTime);
        });
    }

    //=======================================================
    // 内部状态操作
    //=======================================================
    private static String getStates() {
        return String.format("currPage=%s, handledPage=%s, " +
                        "scrollStart=%s, scrollStop=%s\n%s",
                getCurrPage(), getHandledPage(),
                scrollStart, scrollStop, chatStats.toString());
    }

    private static boolean isRobotClicked() {
        return chatStats.isRobot();
    }

    private static void resetChatStats() {
        chatStats.reset();
        Logger.e("重置 %s", chatStats.toString());
    }

    private static void setHandledPage(String p) {
        if (isPageChanged()) {
            handledPage = p;
            Logger.e("更新 handledPage 为 %s : currPage=%s", handledPage, getCurrPage());
        }
    }

    private static String getHandledPage() {
        return handledPage;
    }

    private static void setCurrentPage(String p) {
        currPage = p;
        Logger.e("更新 currPage 为当前页面 %s : handledPage=%s", p, getHandledPage());
    }

    private static void setCurrentPageUnknown() {
        if (!Arrays.asList(RedPacketGrabPage, RedPacketDetailPage).contains(getCurrPage())) {
            currPage = UnknownPage;
            Logger.e("更新 currPage 为 %s : handledPage=%s",
                    getCurrPage(), getHandledPage());
        } else {
            Logger.e("更新 currPage 为 %s，无法设为 Unknown : handledPage=%s",
                    getCurrPage(), getHandledPage());
        }
    }

    private static String getCurrPage() {
        return currPage;
    }
}
