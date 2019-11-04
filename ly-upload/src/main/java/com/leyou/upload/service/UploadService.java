package com.leyou.upload.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.leyou.upload.properties.OSSProperties;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class UploadService {

    //保存图片的地址
    private static final String IMAGE_PATH = "E:\\nginx-1.17.3\\html\\img-file";

    //浏览器访 图片的地址
    private static final String IMAGE_URL = "http://image.leyou.com/img-file/";

    //允许图片上传的格式
    private static final List ALLOW_UPLOAD_FILE = Arrays.asList("image/jpeg");

    @Autowired
    private OSS client;

    @Autowired
    private OSSProperties prop;


    /**
     * 本地上传文件
     * @param file
     * @return
     */
    public String localUploadFile(MultipartFile file) {

        //判断上传 是否是jpg
        if (!ALLOW_UPLOAD_FILE.contains(file.getContentType())){
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }

        //是jpg 解析图片元素
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }

        //判断是否是空
        if (bufferedImage==null){
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }

        //指定上传的file名称
        String imageName = UUID.randomUUID()+file.getOriginalFilename();

        //得到上传file的对象
        File imagePathFile = new File(IMAGE_PATH);

        //本地上传
        try {
            file.transferTo(new File(imagePathFile,imageName));
        } catch (IOException e) {
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        }

        return IMAGE_URL+imageName;
    }

    /**
     * 阿里云OSS上传文件
     * @return
     */
    public Map<String, Object> getOssSignature() {
        try {
            long expireTime = prop.getExpireTime();
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, prop.getMaxFileSize());
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, prop.getDir());

            String postPolicy = client.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = client.calculatePostSignature(postPolicy);

            Map<String, Object> respMap = new LinkedHashMap<>();
            respMap.put("accessId", prop.getAccessKeyId());
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", prop.getDir());
            respMap.put("host", prop.getHost());
            respMap.put("expire", expireEndTime);
            return respMap;
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        }

    }
}
