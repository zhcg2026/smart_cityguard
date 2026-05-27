package com.cityguard.task.dto;

import lombok.Data;

@Data
public class TaskAttachmentView {

    private Long id;

    private String fileType;

    private String fileName;

    private String filePath;
}
