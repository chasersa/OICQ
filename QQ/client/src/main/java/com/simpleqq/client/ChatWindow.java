package com.simpleqq.client;

import com.simpleqq.common.Message;
import com.simpleqq.common.MessageType;
import com.simpleqq.common.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ChatWindow extends JFrame {
    private Client client;
    private JTabbedPane tabbedPane;
    private JList<String> friendList;
    private JList<String> onlineUserList;
    private JList<String> groupList; // New: Group List
    private DefaultListModel<String> friendListModel;
    private DefaultListModel<String> onlineUserListModel;
    private DefaultListModel<String> groupListModel; // New: Group List Model
    private Map<String, SingleChatWindow> singleChatWindows;
    private Map<String, GroupChatWindow> groupChatWindows;

    // New: Request Panel components
    private JPanel requestPanel;
    private DefaultListModel<String> friendRequestListModel;
    private JList<String> friendRequestList;
    private DefaultListModel<String> groupInviteListModel;
    private JList<String> groupInviteList;

    public ChatWindow(Client client) {
        this.client = client;
        this.singleChatWindows = new HashMap<>();
        this.groupChatWindows = new HashMap<>();

        setTitle("QQ - " + client.getCurrentUser().getUsername() + " (" + client.getCurrentUser().getId() + ")");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // Left Panel for Friends, Online Users, Groups, and Requests
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        tabbedPane = new JTabbedPane();

        // Friends Tab
        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFriend = friendList.getSelectedValue();
                if (selectedFriend != null) {
                    String friendId = selectedFriend.split(" ")[0];
                    openSingleChatWindow(friendId);
                }
            }
        });
        tabbedPane.addTab("好友", new JScrollPane(friendList));

        // Online Users Tab
        onlineUserListModel = new DefaultListModel<>();
        onlineUserList = new JList<>(onlineUserListModel);
        onlineUserList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineUserList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = onlineUserList.getSelectedValue();
                if (selectedUser != null) {
                    String userId = selectedUser.split(" ")[0];
                    if (!userId.equals(client.getCurrentUser().getId())) { // Cannot chat with self
                        openSingleChatWindow(userId);
                    }
                }
            }
        });
        tabbedPane.addTab("在线用户", new JScrollPane(onlineUserList));

        // Groups Tab
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedGroup = groupList.getSelectedValue();
                if (selectedGroup != null) {
                    String groupId = selectedGroup.split(" ")[0];
                    openGroupChatWindow(groupId);
                }
            }
        });
        tabbedPane.addTab("群聊", new JScrollPane(groupList));

        // Requests Tab
        requestPanel = new JPanel(new BorderLayout());
        JTabbedPane requestTabbedPane = new JTabbedPane();

        // Friend Requests
        friendRequestListModel = new DefaultListModel<>();
        friendRequestList = new JList<>(friendRequestListModel);
        friendRequestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTabbedPane.addTab("好友请求", new JScrollPane(friendRequestList));

        // Group Invites
        groupInviteListModel = new DefaultListModel<>();
        groupInviteList = new JList<>(groupInviteListModel);
        groupInviteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTabbedPane.addTab("群聊邀请", new JScrollPane(groupInviteList));

        requestPanel.add(requestTabbedPane, BorderLayout.CENTER);

        JPanel requestButtonPanel = new JPanel(new GridLayout(2, 2));
        JButton acceptFriendRequestButton = new JButton("接受好友");
        JButton rejectFriendRequestButton = new JButton("拒绝好友");
        JButton acceptGroupInviteButton = new JButton("接受群聊");
        JButton rejectGroupInviteButton = new JButton("拒绝群聊");

        acceptFriendRequestButton.addActionListener(e -> handleFriendRequestAction(true));
        rejectFriendRequestButton.addActionListener(e -> handleFriendRequestAction(false));
        acceptGroupInviteButton.addActionListener(e -> handleGroupInviteAction(true));
        rejectGroupInviteButton.addActionListener(e -> handleGroupInviteAction(false));

        requestButtonPanel.add(acceptFriendRequestButton);
        requestButtonPanel.add(rejectFriendRequestButton);
        requestButtonPanel.add(acceptGroupInviteButton);
        requestButtonPanel.add(rejectGroupInviteButton);
        requestPanel.add(requestButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("请求", requestPanel);

        leftPanel.add(tabbedPane, BorderLayout.CENTER);

        // Buttons for Add/Delete Friend, Create Group
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1));
        JButton addFriendButton = new JButton("添加好友");
        JButton deleteFriendButton = new JButton("删除好友");
        JButton createGroupButton = new JButton("创建群聊");

        addFriendButton.addActionListener(e -> {
            String friendId = JOptionPane.showInputDialog(this, "请输入好友ID:");
            if (friendId != null && !friendId.trim().isEmpty()) {
                if (friendId.equals(client.getCurrentUser().getId())) {
                    JOptionPane.showMessageDialog(this, "不能添加自己为好友！");
                    return;
                }
                client.sendMessage(new Message(MessageType.FRIEND_REQUEST, client.getCurrentUser().getId(), friendId, ""));
                JOptionPane.showMessageDialog(this, "好友请求已发送！");
            }
        });

        deleteFriendButton.addActionListener(e -> {
            String selectedFriend = friendList.getSelectedValue();
            if (selectedFriend != null) {
                String friendId = selectedFriend.split(" ")[0];
                int confirm = JOptionPane.showConfirmDialog(this, "确定要删除好友 " + friendId + " 吗？", "删除好友", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    client.sendMessage(new Message(MessageType.DELETE_FRIEND, client.getCurrentUser().getId(), friendId, ""));
                }
            } else {
                JOptionPane.showMessageDialog(this, "请选择要删除的好友。");
            }
        });

        createGroupButton.addActionListener(e -> {
            String groupId = JOptionPane.showInputDialog(this, "请输入群聊ID:");
            if (groupId != null && !groupId.trim().isEmpty()) {
                // Send a message to server to create group
                client.sendMessage(new Message(MessageType.CREATE_GROUP, client.getCurrentUser().getId(), "Server", groupId));
            }
        });

        buttonPanel.add(addFriendButton);
        buttonPanel.add(deleteFriendButton);
        buttonPanel.add(createGroupButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.WEST);

        // Right Panel (Placeholder for chat area)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("请选择一个好友或群组开始聊天", SwingConstants.CENTER), BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        // Handle incoming messages
        client.setMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                handleIncomingMessage(message);
            });
        });

        // Request initial data
        client.sendMessage(new Message(MessageType.FRIEND_LIST, client.getCurrentUser().getId(), "Server", ""));
        client.sendMessage(new Message(MessageType.ONLINE_USERS, client.getCurrentUser().getId(), "Server", ""));
        client.sendMessage(new Message(MessageType.GET_GROUPS, client.getCurrentUser().getId(), "Server", "")); // Request groups
        client.sendMessage(new Message(MessageType.GET_PENDING_REQUESTS, client.getCurrentUser().getId(), "Server", "")); // Request pending requests

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.disconnect();
            }
        });
    }

    private void handleIncomingMessage(Message message) {
        switch (message.getType()) {
            case FRIEND_LIST:
                updateFriendList(message.getContent());
                break;
            case ONLINE_USERS:
                updateOnlineUserList(message.getContent());
                break;
            case TEXT_MESSAGE:
            case IMAGE_MESSAGE:
            case IMAGE_DATA:
                // Find or create chat window and display message
                String sender = message.getSenderId();
                String receiver = message.getReceiverId();
                String currentUserId = client.getCurrentUser().getId();

                if (receiver.equals(currentUserId)) { // Single chat message to current user
                    openSingleChatWindow(sender).displayMessage(message);
                } else if (sender.equals(currentUserId)) { // Single chat message from current user
                    openSingleChatWindow(receiver).displayMessage(message);
                } else { // Group message
                    openGroupChatWindow(receiver).displayMessage(message);
                }
                break;
            case GROUP_MESSAGE:
                // 处理群聊消息
                openGroupChatWindow(message.getReceiverId()).displayMessage(message);
                break;
            case IMAGE_REQUEST:
                handleImageRequest(message);
                break;
            case IMAGE_ACCEPT:
            case IMAGE_REJECT:
                // These messages are for the sender, pass to SingleChatWindow
                openSingleChatWindow(message.getReceiverId()).handleImageMessage(message);
                break;
            case ADD_FRIEND_SUCCESS:
                JOptionPane.showMessageDialog(this, "添加好友成功: " + message.getContent());
                client.sendMessage(new Message(MessageType.FRIEND_LIST, client.getCurrentUser().getId(), "Server", "")); // Refresh friend list
                break;
            case ADD_FRIEND_FAIL:
                JOptionPane.showMessageDialog(this, "添加好友失败: " + message.getContent());
                break;
            case DELETE_FRIEND_SUCCESS:
                JOptionPane.showMessageDialog(this, "删除好友成功: " + message.getContent());
                client.sendMessage(new Message(MessageType.FRIEND_LIST, client.getCurrentUser().getId(), "Server", "")); // Refresh friend list
                break;
            case DELETE_FRIEND_FAIL:
                JOptionPane.showMessageDialog(this, "删除好友失败: " + message.getContent());
                break;
            case SERVER_MESSAGE:
                JOptionPane.showMessageDialog(this, "服务器消息: " + message.getContent());
                break;
            case FRIEND_REQUEST:
                // Content is senderId
                String friendRequestSenderId = message.getSenderId();
                if (!friendRequestListModel.contains(friendRequestSenderId)) {
                    friendRequestListModel.addElement(friendRequestSenderId);
                    JOptionPane.showMessageDialog(this, "您收到一条好友请求来自: " + friendRequestSenderId);
                }
                break;
            case FRIEND_ACCEPT:
                JOptionPane.showMessageDialog(this, message.getSenderId() + " 接受了您的好友请求。");
                client.sendMessage(new Message(MessageType.FRIEND_LIST, client.getCurrentUser().getId(), "Server", "")); // Refresh friend list
                break;
            case FRIEND_REJECT:
                JOptionPane.showMessageDialog(this, message.getSenderId() + " 拒绝了您的好友请求。");
                break;
            case GROUP_INVITE:
                // Content is groupId
                String groupInviteGroupId = message.getContent();
                String inviterId = message.getSenderId();
                String inviteDisplay = groupInviteGroupId + " (来自 " + inviterId + ")";
                if (!groupInviteListModel.contains(inviteDisplay)) {
                    groupInviteListModel.addElement(inviteDisplay);
                    JOptionPane.showMessageDialog(this, "您收到一条群聊邀请来自 " + inviterId + " 加入群聊: " + groupInviteGroupId);
                }
                break;
            case GET_GROUPS:
                updateGroupList(message.getContent());
                break;
            case GET_PENDING_REQUESTS:
                updatePendingRequests(message.getContent());
                break;
            case CREATE_GROUP_SUCCESS:
                JOptionPane.showMessageDialog(this, "群聊创建成功: " + message.getContent());
                client.sendMessage(new Message(MessageType.GET_GROUPS, client.getCurrentUser().getId(), "Server", ""));
                break;
            case CREATE_GROUP_FAIL:
                JOptionPane.showMessageDialog(this, "群聊创建失败: " + message.getContent());
                break;
            case GROUP_JOIN_SUCCESS:
                JOptionPane.showMessageDialog(this, "成功加入群聊: " + message.getContent());
                client.sendMessage(new Message(MessageType.GET_GROUPS, client.getCurrentUser().getId(), "Server", "")); // Refresh group list
                break;
            case GROUP_JOIN_FAIL:
                JOptionPane.showMessageDialog(this, "加入群聊失败: " + message.getContent());
                break;
            case GET_GROUP_MEMBERS:
                // 更新群成员列表
                String groupId = message.getSenderId(); // 群ID在senderId字段
                GroupChatWindow groupWindow = groupChatWindows.get(groupId);
                if (groupWindow != null) {
                    String membersStr = message.getContent();
                    java.util.List<String> members = new java.util.ArrayList<>();
                    if (membersStr != null && !membersStr.isEmpty()) {
                        String[] memberInfos = membersStr.split(";");
                        for (String memberInfo : memberInfos) {
                            String[] parts = memberInfo.split(":");
                            if (parts.length >= 2) {
                                members.add(parts[0] + " " + parts[1]); // ID + username
                            }
                        }
                    }
                    groupWindow.updateGroupMembers(members);
                }
                break;
            default:
                System.out.println("Unhandled message type in ChatWindow: " + message.getType());
        }
    }

    private void handleImageRequest(Message message) {
        String senderId = message.getSenderId();
        String fileName = message.getContent();

        int choice = JOptionPane.showConfirmDialog(this,
                senderId + " 想向您发送图片: " + fileName + "。是否接受？",
                "接收图片请求",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setDialogTitle("选择图片保存路径");
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int folderChoice = folderChooser.showSaveDialog(this);
            if (folderChoice == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = folderChooser.getSelectedFile();
                String savePath = selectedFolder.getAbsolutePath() + File.separator + fileName;
                Message acceptMessage = new Message(MessageType.IMAGE_ACCEPT, client.getCurrentUser().getId(), senderId, savePath);
                client.sendMessage(acceptMessage);
                JOptionPane.showMessageDialog(this, "已接受图片请求，请等待图片传输。");
            } else {
                // User cancelled folder selection, reject the image
                Message rejectMessage = new Message(MessageType.IMAGE_REJECT, client.getCurrentUser().getId(), senderId, fileName);
                client.sendMessage(rejectMessage);
                JOptionPane.showMessageDialog(this, "您已取消图片保存，图片请求已拒绝。");
            }
        } else {
            Message rejectMessage = new Message(MessageType.IMAGE_REJECT, client.getCurrentUser().getId(), senderId, fileName);
            client.sendMessage(rejectMessage);
            JOptionPane.showMessageDialog(this, "您已拒绝图片请求。");
        }
    }

    private void updateFriendList(String friendListStr) {
        friendListModel.clear();
        if (friendListStr != null && !friendListStr.isEmpty()) {
            String[] friends = friendListStr.split(";");
            for (String friendInfo : friends) {
                String[] parts = friendInfo.split(":");
                if (parts.length == 3) {
                    String id = parts[0];
                    String username = parts[1];
                    String status = parts[2];
                    friendListModel.addElement(id + " " + username + " (" + status + ")");
                }
            }
        }
    }

    private void updateOnlineUserList(String onlineUserListStr) {
        onlineUserListModel.clear();
        if (onlineUserListStr != null && !onlineUserListStr.isEmpty()) {
            String[] users = onlineUserListStr.split(";");
            for (String userInfo : users) {
                String[] parts = userInfo.split(":");
                if (parts.length == 2) {
                    String id = parts[0];
                    String username = parts[1];
                    onlineUserListModel.addElement(id + " " + username);
                }
            }
        }
    }

    private void updateGroupList(String groupListStr) {
        groupListModel.clear();
        if (groupListStr != null && !groupListStr.isEmpty()) {
            String[] groups = groupListStr.split(";");
            for (String groupId : groups) {
                groupListModel.addElement(groupId);
            }
        }
    }

    private void updatePendingRequests(String pendingRequestsStr) {
        friendRequestListModel.clear();
        groupInviteListModel.clear();

        if (pendingRequestsStr != null && !pendingRequestsStr.isEmpty()) {
            String[] parts = pendingRequestsStr.split("\\|\\|");
            String friendRequests = parts.length > 0 ? parts[0] : "";
            String groupInvites = parts.length > 1 ? parts[1] : "";

            if (!friendRequests.isEmpty()) {
                String[] requests = friendRequests.split(";");
                for (String senderId : requests) {
                    friendRequestListModel.addElement(senderId);
                }
            }

            if (!groupInvites.isEmpty()) {
                String[] invites = groupInvites.split(";");
                for (String inviteInfo : invites) {
                    // Assuming inviteInfo is just groupId for now, need to adjust if senderId is also passed
                    groupInviteListModel.addElement(inviteInfo);
                }
            }
        }
    }

    private SingleChatWindow openSingleChatWindow(String friendId) {
        SingleChatWindow chatWindow = singleChatWindows.get(friendId);
        if (chatWindow == null) {
            chatWindow = new SingleChatWindow(client, friendId);
            singleChatWindows.put(friendId, chatWindow);
            chatWindow.setVisible(true);
            chatWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    singleChatWindows.remove(friendId);
                }
            });
        } else {
            chatWindow.toFront(); // Bring to front if already open
        }
        return chatWindow;
    }

    private GroupChatWindow openGroupChatWindow(String groupId) {
        GroupChatWindow chatWindow = groupChatWindows.get(groupId);
        if (chatWindow == null) {
            chatWindow = new GroupChatWindow(client, groupId);
            groupChatWindows.put(groupId, chatWindow);
            chatWindow.setVisible(true);
            chatWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    groupChatWindows.remove(groupId);
                }
            });
        } else {
            chatWindow.toFront();
        }
        return chatWindow;
    }

    private void handleFriendRequestAction(boolean accept) {
        String selectedRequest = friendRequestList.getSelectedValue();
        if (selectedRequest != null) {
            String senderId = selectedRequest;
            if (accept) {
                client.sendMessage(new Message(MessageType.FRIEND_ACCEPT, client.getCurrentUser().getId(), senderId, ""));
            } else {
                client.sendMessage(new Message(MessageType.FRIEND_REJECT, client.getCurrentUser().getId(), senderId, ""));
            }
            friendRequestListModel.removeElement(selectedRequest);
        } else {
            JOptionPane.showMessageDialog(this, "请选择一个好友请求。");
        }
    }

    private void handleGroupInviteAction(boolean accept) {
        String selectedInvite = groupInviteList.getSelectedValue();
        if (selectedInvite != null) {
            String groupId = selectedInvite.split(" \\(")[0]; // Extract groupId from "groupId (from senderId)"
            String inviterId = selectedInvite.substring(selectedInvite.indexOf("来自 ") + 3, selectedInvite.indexOf(")"));

            if (accept) {
                client.sendMessage(new Message(MessageType.GROUP_ACCEPT, client.getCurrentUser().getId(), inviterId, groupId));
            } else {
                client.sendMessage(new Message(MessageType.GROUP_REJECT, client.getCurrentUser().getId(), inviterId, groupId));
            }
            groupInviteListModel.removeElement(selectedInvite);
        } else {
            JOptionPane.showMessageDialog(this, "请选择一个群聊邀请。");
        }
    }
}