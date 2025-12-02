# Details

Date : 2025-11-27 16:36:05

Directory /Users/ryllis/IdeaProjects/chat/src/main/java

Total : 30 files,  861 codes, 161 comments, 223 blanks, all 1245 lines

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [src/main/java/com/example/chat/ChatApplication.java](/src/main/java/com/example/chat/ChatApplication.java) | Java | 9 | 0 | 5 | 14 |
| [src/main/java/com/example/chat/common/model/Group.java](/src/main/java/com/example/chat/common/model/Group.java) | Java | 11 | 1 | 2 | 14 |
| [src/main/java/com/example/chat/common/model/Message.java](/src/main/java/com/example/chat/common/model/Message.java) | Java | 24 | 4 | 6 | 34 |
| [src/main/java/com/example/chat/common/model/User.java](/src/main/java/com/example/chat/common/model/User.java) | Java | 25 | 6 | 10 | 41 |
| [src/main/java/com/example/chat/common/packet/WsRequest.java](/src/main/java/com/example/chat/common/packet/WsRequest.java) | Java | 8 | 0 | 2 | 10 |
| [src/main/java/com/example/chat/common/packet/WsResponse.java](/src/main/java/com/example/chat/common/packet/WsResponse.java) | Java | 16 | 1 | 3 | 20 |
| [src/main/java/com/example/chat/config/AppConfig.java](/src/main/java/com/example/chat/config/AppConfig.java) | Java | 25 | 7 | 6 | 38 |
| [src/main/java/com/example/chat/config/AuthHandshakeInterceptor.java](/src/main/java/com/example/chat/config/AuthHandshakeInterceptor.java) | Java | 28 | 12 | 9 | 49 |
| [src/main/java/com/example/chat/config/WebSocketConfig.java](/src/main/java/com/example/chat/config/WebSocketConfig.java) | Java | 19 | 2 | 4 | 25 |
| [src/main/java/com/example/chat/handler/ChatHandler.java](/src/main/java/com/example/chat/handler/ChatHandler.java) | Java | 69 | 17 | 14 | 100 |
| [src/main/java/com/example/chat/handler/HandlerRegistry.java](/src/main/java/com/example/chat/handler/HandlerRegistry.java) | Java | 22 | 15 | 7 | 44 |
| [src/main/java/com/example/chat/handler/action/ActionHandler.java](/src/main/java/com/example/chat/handler/action/ActionHandler.java) | Java | 8 | 12 | 7 | 27 |
| [src/main/java/com/example/chat/handler/action/BaseActionHandler.java](/src/main/java/com/example/chat/handler/action/BaseActionHandler.java) | Java | 28 | 15 | 7 | 50 |
| [src/main/java/com/example/chat/handler/action/BaseAdminActionHandler.java](/src/main/java/com/example/chat/handler/action/BaseAdminActionHandler.java) | Java | 20 | 8 | 4 | 32 |
| [src/main/java/com/example/chat/handler/action/CreateGroupHandler.java](/src/main/java/com/example/chat/handler/action/CreateGroupHandler.java) | Java | 38 | 0 | 9 | 47 |
| [src/main/java/com/example/chat/handler/action/GetOnlineHandler.java](/src/main/java/com/example/chat/handler/action/GetOnlineHandler.java) | Java | 31 | 0 | 8 | 39 |
| [src/main/java/com/example/chat/handler/action/HeartbeatHandler.java](/src/main/java/com/example/chat/handler/action/HeartbeatHandler.java) | Java | 15 | 7 | 6 | 28 |
| [src/main/java/com/example/chat/handler/action/HistoryHandler.java](/src/main/java/com/example/chat/handler/action/HistoryHandler.java) | Java | 38 | 5 | 9 | 52 |
| [src/main/java/com/example/chat/handler/action/KickUserHandler.java](/src/main/java/com/example/chat/handler/action/KickUserHandler.java) | Java | 35 | 2 | 9 | 46 |
| [src/main/java/com/example/chat/handler/action/LoginHandler.java](/src/main/java/com/example/chat/handler/action/LoginHandler.java) | Java | 60 | 14 | 15 | 89 |
| [src/main/java/com/example/chat/handler/action/MessageReactHandler.java](/src/main/java/com/example/chat/handler/action/MessageReactHandler.java) | Java | 53 | 4 | 13 | 70 |
| [src/main/java/com/example/chat/handler/action/MessageReadHandler.java](/src/main/java/com/example/chat/handler/action/MessageReadHandler.java) | Java | 45 | 2 | 11 | 58 |
| [src/main/java/com/example/chat/handler/action/MuteUserHandler.java](/src/main/java/com/example/chat/handler/action/MuteUserHandler.java) | Java | 36 | 3 | 10 | 49 |
| [src/main/java/com/example/chat/handler/action/RecallMessageHandler.java](/src/main/java/com/example/chat/handler/action/RecallMessageHandler.java) | Java | 40 | 2 | 11 | 53 |
| [src/main/java/com/example/chat/handler/action/SendMessageHandler.java](/src/main/java/com/example/chat/handler/action/SendMessageHandler.java) | Java | 72 | 11 | 14 | 97 |
| [src/main/java/com/example/chat/repository/DataCenter.java](/src/main/java/com/example/chat/repository/DataCenter.java) | Java | 13 | 4 | 6 | 23 |
| [src/main/java/com/example/chat/service/MessageService.java](/src/main/java/com/example/chat/service/MessageService.java) | Java | 6 | 1 | 2 | 9 |
| [src/main/java/com/example/chat/service/UserService.java](/src/main/java/com/example/chat/service/UserService.java) | Java | 8 | 0 | 2 | 10 |
| [src/main/java/com/example/chat/service/impl/MessageServiceImpl.java](/src/main/java/com/example/chat/service/impl/MessageServiceImpl.java) | Java | 27 | 2 | 7 | 36 |
| [src/main/java/com/example/chat/service/impl/UserServiceImpl.java](/src/main/java/com/example/chat/service/impl/UserServiceImpl.java) | Java | 32 | 4 | 5 | 41 |

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)