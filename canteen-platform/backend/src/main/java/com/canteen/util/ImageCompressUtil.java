package com.canteen.util;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** 图片压缩工具 */
public class ImageCompressUtil {

    /**
     * 压缩图片到指定宽度, 指定 JPEG 质量
     * @param in 输入流
     * @param maxWidth 最大宽度(px)
     * @param quality 质量 0-1
     */
    public static byte[] compress(InputStream in, int maxWidth, double quality) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Thumbnails.of(in)
                    .width(maxWidth)
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toOutputStream(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("图片压缩失败: " + e.getMessage(), e);
        }
    }
}
