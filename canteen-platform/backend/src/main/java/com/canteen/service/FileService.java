package com.canteen.service;

import com.canteen.util.ImageCompressUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** 图片上传与压缩 */
@Service
public class FileService {

    @Value("${canteen.upload-dir:uploads/}")
    private String uploadDir;

    @Value("${canteen.base-url:/upload/}")
    private String baseUrl;

    /**
     * 保存图片并按规格压缩
     * @param file 上传文件
     * @param maxWidth 目标宽度
     * @param quality 质量 0-1
     * @return 可访问的相对URL
     */
    public String saveImage(MultipartFile file, int maxWidth, double quality) throws IOException {
        String ext = ".jpg";
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        byte[] bytes;
        if (file == null || file.isEmpty()) {
            throw new IOException("文件为空");
        }
        try {
            bytes = ImageCompressUtil.compress(file.getInputStream(), maxWidth, quality);
        } catch (Exception e) {
            // 非图片文件，原样保存
            bytes = file.getBytes();
            String orig = file.getOriginalFilename();
            if (orig != null && orig.contains(".")) {
                ext = orig.substring(orig.lastIndexOf('.'));
                filename = UUID.randomUUID().toString().replace("-", "") + ext;
            }
        }
        Files.write(dir.resolve(filename), bytes);
        return baseUrl + filename;
    }

    public String saveRaw(MultipartFile file) throws IOException {
        String orig = file.getOriginalFilename() == null ? ".jpg" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')) : ".jpg";
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), file.getBytes());
        return baseUrl + filename;
    }

    /** 保存字节图片并压缩(Excel 导入用)，返回可访问URL */
    public String saveImageBytes(byte[] data, int maxWidth, double quality) throws IOException {
        if (data == null || data.length == 0) throw new IOException("图片为空");
        String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        byte[] out;
        try {
            out = ImageCompressUtil.compress(new java.io.ByteArrayInputStream(data), maxWidth, quality);
        } catch (Exception e) {
            out = data;
        }
        Files.write(dir.resolve(filename), out);
        return baseUrl + filename;
    }

    /** 按 /upload/xxx.jpg 读取本地图片字节(Excel 导出用)，不存在返回 null */
    public byte[] readImageBytes(String url) {
        if (url == null || url.isEmpty()) return null;
        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename.isEmpty()) return null;
        try {
            Path p = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            if (!java.nio.file.Files.exists(p)) return null;
            return java.nio.file.Files.readAllBytes(p);
        } catch (IOException e) {
            return null;
        }
    }

    /** 删除 /upload/xxx.jpg 对应的本地图片文件，成功删除返回 true */
    public boolean deleteImage(String url) {
        if (url == null || url.isEmpty()) return false;
        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename.isEmpty()) return false;
        try {
            Path p = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            return java.nio.file.Files.deleteIfExists(p);
        } catch (IOException e) {
            return false;
        }
    }
}
