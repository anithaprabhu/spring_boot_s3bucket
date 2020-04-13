package com.example.bucket.controller;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.example.bucket.service.AmazonClient;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage/")
public class BucketController {

    private AmazonClient amazonClient;

    @Autowired
    BucketController(AmazonClient amazonClient) {
        this.amazonClient = amazonClient;
    }

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestPart(value = "file") MultipartFile file) {
        return this.amazonClient.uploadFile(file);
    }

    @DeleteMapping("/deleteFile")
    public String deleteFile(@RequestPart(value = "url") String fileUrl) {
        return this.amazonClient.deleteFileFromS3Bucket(fileUrl);
    }
    
    
    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
    	
    	S3Object s3FileObj = this.amazonClient.downloadFile(fileName);    	
    	ObjectMetadata metaData = s3FileObj.getObjectMetadata();
    	String contentType = metaData.getContentType();
    	long contentLength = metaData.getContentLength();
        System.out.println("File Type is :"+contentType);
        
        S3ObjectInputStream stream = s3FileObj.getObjectContent();
        byte[] content = null;
        ByteArrayResource resource = null;
        try {
        	 content = IOUtils.toByteArray(stream);
        	 resource = new ByteArrayResource(content);
        	 s3FileObj.close();           
         } catch (IOException e) {
             e.printStackTrace();
         }
               
        return ResponseEntity
        		.ok()
        		.contentLength(contentLength)
        		.contentType(MediaType.parseMediaType(contentType))
                .header("Content-disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
