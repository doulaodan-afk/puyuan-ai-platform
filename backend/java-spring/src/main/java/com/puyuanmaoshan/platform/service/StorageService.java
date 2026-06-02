package com.puyuanmaoshan.platform.service;

import java.io.InputStream;

/**
 * 对象存储服务接口
 * 支持多配置轮换
 */
public interface StorageService {

    /**
     * 上传文件
     * @param objectKey 对象键（文件路径）
     * @param inputStream 文件输入流
     * @param contentLength 文件大小
     * @return 文件访问 URL
     */
    String uploadFile(String objectKey, InputStream inputStream, long contentLength);

    /**
     * 上传文件（指定内容类型）
     * @param objectKey 对象键（文件路径）
     * @param inputStream 文件输入流
     * @param contentLength 文件大小
     * @param contentType 内容类型
     * @return 文件访问 URL
     */
    String uploadFile(String objectKey, InputStream inputStream, long contentLength, String contentType);

    /**
     * 下载文件
     * @param objectKey 对象键（文件路径）
     * @return 文件输入流
     */
    InputStream downloadFile(String objectKey);

    /**
     * 删除文件
     * @param objectKey 对象键（文件路径）
     */
    void deleteFile(String objectKey);

    /**
     * 获取文件 URL（签名 URL）
     * @param objectKey 对象键（文件路径）
     * @param expiresIn 过期时间（秒）
     * @return 签名 URL
     */
    String getSignedUrl(String objectKey, int expiresIn);

    /**
     * 获取文件 URL（公共读）
     * @param objectKey 对象键（文件路径）
     * @return 公共 URL
     */
    String getPublicUrl(String objectKey);

    /**
     * 检查文件是否存在
     * @param objectKey 对象键（文件路径）
     * @return 是否存在
     */
    boolean fileExists(String objectKey);

    /**
     * 获取当前可用的 OSS 配置数量
     * @return 配置数量
     */
    int getAvailableConfigCount();

    /**
     * 测试 OSS 连接
     * @return 测试结果
     */
    boolean testConnection();
}
