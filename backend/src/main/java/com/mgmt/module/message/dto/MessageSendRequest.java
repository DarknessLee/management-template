package com.mgmt.module.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MessageSendRequest {

    /** all：全员；role：按角色；user：指定用户 */
    @NotBlank(message = "{validation.messageScopeRequired}")
    private String scope;

    private List<Long> roleIds;

    private List<Long> userIds;

    @NotBlank(message = "{validation.messageTitleRequired}")
    @Size(max = 100, message = "{validation.messageTitleTooLong}")
    private String title;

    @NotBlank(message = "{validation.messageContentRequired}")
    @Size(max = 500, message = "{validation.messageContentTooLong}")
    private String content;

    @Size(max = 200, message = "{validation.messageLinkTooLong}")
    private String link;
}
