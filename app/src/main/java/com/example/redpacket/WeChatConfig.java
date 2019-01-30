package com.example.redpacket;

public class WeChatConfig {

    //=======================================================
    // 微信版本：7.0.3
    //=======================================================

    //=======================================================
    // 过滤关键字
    //=======================================================
    // @ 红包关键字
    //   用于聊天列表页面列表项消息内容的过滤
    //  用于通知栏消息内容的过滤
    public static final String RedPacketKey = "[微信红包]";

    // @ 红包异常关键字
    //   用于通知栏消息内容的过滤
    public static final String UnNormalRedPacketKey = "红包行为异常";


    //=======================================================
    // 聊天列表页面各种控件的信息
    //=======================================================

    // @ 聊天列表页面Activity的类名
    //   用于检查当前窗体是否包含聊天列表页面
    public static final String ChatListPage_ClassName = "com.tencent.mm.ui.LauncherUI";

    // @ 聊天列表页面名称的ID，名称分四类：微信、通讯录、发现、我
    //   用于检查当前页面是否是聊天列表页面
    public static final String ChatListPage_Name_ID = "android:id/text1";

    // @ 聊天列表页面下拉后出现的小程序快捷页面中的“我的小程序”ID
    //   用于检查当前页面是否是聊天列表页面
    public static final String ChatListPage_MicroApp_Title_ID = "com.tencent.mm:id/p9";

    // @ 聊天列表页面的名称
    //   用于检查聊天列表页面的名称过滤，若不包含，则不是聊天列表页面
    public static final String ChatListPage_Name = "微信";

    // @ 聊天列表项ID
    //   用于获取当前可视窗口所有列表项，无法通过模拟点击进入红包
    public static final String ChatListPage_Item_ID = "com.tencent.mm:id/b5m";

    // @ 聊天列表项中显示的消息内容ID
    //   用于检查当前列表项是否包含 [微信红包] 关键字，若是，则点击进入抢红包
    public static final String ChatListPage_Item_Content_ID = "com.tencent.mm:id/b5q";

    // @ 聊天列表项中显示未读消息数量的ID
    //   用于检查当前一个聊天列表项中是否有未读的消息，可处理：因各类原因造成的消息内容包含 [微信红包] 关键字的情况
    public static final String ChatListPage_Item_MsgCount_ID = "com.tencent.mm:id/mv";


    //=======================================================
    // 聊天会话页面的控件信息
    //=======================================================

    // @ 聊天会话页面类名
    //   用于检查当前窗体是否包含聊天会话页面
    public static final String ChatPage_ClassName = "com.tencent.mm.ui.LauncherUI";

    // 从聊天会话设置按钮进入，点击任一头像，再点击发消息，会触发TYPE_WINDOW_STATE_CHANGED
    // 这时，聊天会话页面的类名显示为 com.tencent.mm.ui.chatting.ChattingUI
    public static final String ChatPage_ClassName1 = "com.tencent.mm.ui.chatting.ChattingUI";

    // @ 返回按钮ID
    //   用于模拟点击，返回聊天列表页面
    //   用于补充检查页面发生转换,但未触发 WINDOW_STATE_CHANGED事件的情况
    public static final String ChatPage_Back_ID = "com.tencent.mm:id/k2";

    // @ 聊天会话名称ID
    //   用于检查是否是同一个聊天会话，若不是，则重新抢红包
    public static final String ChatPage_Name_ID = "com.tencent.mm:id/k3";

    //   聊天设置按钮ID
    public static final String ChatPage_Setting_ID = "com.tencent.mm:id/jy";

    // @ 红包布局ID
    //   用于获取所有红包
    public static final String ChatPage_RedPacket_Id = "com.tencent.mm:id/aou";

    // @ 红包状态ID, 显示领取过的红包的状态
    //   用于检查此红包已被领取
    public static final String ChatPage_RedPacket_State_ID = "com.tencent.mm:id/aq6";

    // @ 红包标题ID，在底部显示微信红包四个字
    //   用于模拟点击，自动打开红包
    public static final String ChatPage_RedPacket_Open_ID = "com.tencent.mm:id/aq7";


    //=======================================================
    // 抢红包页面的控件信息
    //=======================================================

    // @ 抢红包页面的类名
    //   用于检查当前窗体是否是抢红包页面
    public static final String RedPacketGrabPage_ClassName = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";

    // @ 红包内容ID
    //   用于检查红包是否已被抢完，若未抢完，则模拟点击，抢红包
    public static final String RedPacketGrabPage_Content_ID = "com.tencent.mm:id/cye";

    // @ 红包内容过滤关键字
    public static final String RedPacketGrabPage_Content_RedPacketOver = "红包派完了";
    public static final String RedPacketGrabPage_Content_RedPacketExpired = "已超过24小时";

    // @ 抢红包按钮ID
    //   用于模拟点击，抢红包
    public static final String RedPacketGrabPage_Grab_ID = "com.tencent.mm:id/cyf";

    // @ 关闭按钮ID
    //   用于模拟点击，关闭抢红包页面，返回聊天会话页面
    public static final String RedPacketGrabPage_Back_ID = "com.tencent.mm:id/cv0";


    //=======================================================
    // 红包详情页面的控件信息
    //=======================================================

    // @ 红包详情页面的类名
    //   用于检查当前窗体是否是红包详情页面
    public static final String RedPacketDetailPage_ClassName = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    // @ 返回按钮ID
    //   用于模拟点击，返回聊天列表页面
    public static final String RedPacketDetailPage_Back_ID = "com.tencent.mm:id/kb";
}
