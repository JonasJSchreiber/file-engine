package com.jonasjschreiber.fileengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Image {
    private String filename;
    private String thumbnailName;
    private String thumbnailUrl;
    private String url;
}
