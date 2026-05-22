package com.puyuanmaoshan.platform.service;

/**
 * OSS 文件上传服务
 */
public interface OssService {

    /**
     * 上传字节数组到 OSS
     *
     * @param bytes 文件字节数组
     * @param objectKey 对象键
     * @return 文件访问 URL
     */
    String uploadBytes(byte[] bytes, String objectKey);

    /**
     * 上传文件到 OSS
     *
     * @param fileName 文件名
     * @param objectKey 对象键
     * @return 文件访问 URL
     */
    String uploadFile(String fileName, String objectKey);

    /**
     * 删除 OSS 文件
     *
     * @param objectKey 对象键
     */
    void deleteFile(String objectKey);

    /**
     * 获取文件访问 URL
     *
     * @param objectKey 对象键
     * @return 文件访问 URL
     */
    String getFileUrl(String objectKey);
}