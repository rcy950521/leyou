package com.leyou.upload.controller;

import com.leyou.upload.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @PostMapping("/image")
    public ResponseEntity<String> localUploadFile(@RequestParam("file") MultipartFile file){
       String url = uploadService.localUploadFile(file);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/signature")
    public ResponseEntity<Map<String,Object>> getOssSignature(){

        Map<String , Object> map = uploadService.getOssSignature();

        return ResponseEntity.ok(map);
    }
}
