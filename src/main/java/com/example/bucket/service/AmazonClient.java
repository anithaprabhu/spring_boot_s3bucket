package com.example.bucket.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
//import com.amazonaws.services.s3.model.S3ObjectInputStream;
//import com.amazonaws.util.IOUtils;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.StandardCopyOption;
import java.util.Date;

@Service
public class AmazonClient {

    private AmazonS3 s3uploadclient;
    private AmazonS3 s3downloadclient;

    @Value("${amazonProperties.endpointUrl}")
    private String endpointUrl;
    @Value("${amazonProperties.bucketName}")
    private String bucketName;
    /**
    @Value("${amazonProperties.uploadAccessKey}")
    private String uploadAccessKey;
    @Value("${amazonProperties.uploadSecretKey}")
    private String uploadSecretKey;
    @Value("${amazonProperties.downloadAccessKey}")
    private String downloadAccessKey;
    @Value("${amazonProperties.downloadSecretKey}")
    private String downloadSecretKey;
    **/
    @Value("${amazonProperties.longTermAccessKey}")
    private String longTermAccessKey;
    @Value("${amazonProperties.longTermSecretKey}")
    private String longTermSecretKey;
    @Value("${amazonProperties.s3UploadRole}")
    private String s3UploadRole;
    @Value("${amazonProperties.s3DownloadRole}")
    private String s3DownloadRole;
   // private static final String ROLE_ARN = "arn:aws:iam::670116370961:role/S3_FullAccess_ToMaha";
  

    @PostConstruct
    private void initializeAmazon() {
       // AWSCredentials uploadCredentials = new BasicAWSCredentials(this.uploadAccessKey, this.uploadSecretKey);
       // this.s3uploadclient = new AmazonS3Client(uploadCredentials);
        //AWSCredentials downloadCredentials = new BasicAWSCredentials(this.downloadAccessKey, this.downloadSecretKey);
        //this.s3downloadclient = new AmazonS3Client(downloadCredentials);
        
        // Step 1. Use anitha-s3-user long-term credentials to call the
        // AWS Security Token Service (STS) AssumeRole API, specifying
        // the ARN for the role S3_FullAccess_ToMaha to s3 bucket - ani-multipart-test1.
        	AWSCredentials longTermCredentials_ = new BasicAWSCredentials(this.longTermAccessKey, this.longTermSecretKey);
            AWSSecurityTokenServiceClient stsClient = new
                AWSSecurityTokenServiceClient(longTermCredentials_);
           
            AssumeRoleRequest assumeUploadRequest = new AssumeRoleRequest()
                .withRoleArn(s3UploadRole)
                .withDurationSeconds(3600)
                .withExternalId("anitha_admin")
                .withRoleSessionName("demo");
           
            AssumeRoleResult assumeUploadResult =
            stsClient.assumeRole(assumeUploadRequest);
            
            AssumeRoleRequest assumeDownloadRequest = new AssumeRoleRequest()
                    .withRoleArn(s3DownloadRole)
                    .withDurationSeconds(3600)
                    .withExternalId("anitha_admin")
                    .withRoleSessionName("demo");
               
                AssumeRoleResult assumeDownloadResult =
                stsClient.assumeRole(assumeDownloadRequest);

        // Step 2. AssumeRole returns temporary security credentials for
        // the IAM role.

            BasicSessionCredentials temporaryUploadCredentials =
            new BasicSessionCredentials(
            		assumeUploadResult.getCredentials().getAccessKeyId(),
            		assumeUploadResult.getCredentials().getSecretAccessKey(),
            		assumeUploadResult.getCredentials().getSessionToken());
            
            BasicSessionCredentials temporaryDownloadCredentials =
                    new BasicSessionCredentials(
                    		assumeDownloadResult.getCredentials().getAccessKeyId(),
                    		assumeDownloadResult.getCredentials().getSecretAccessKey(),
                    		assumeDownloadResult.getCredentials().getSessionToken());
                    
            // Step 3. Make S3 API service calls to access S3 bucket
        // using the temporary security credentials for the Assumed Role
        // that were returned in the previous step.
            this.s3uploadclient = new AmazonS3Client(temporaryUploadCredentials);
            this.s3downloadclient = new AmazonS3Client(temporaryDownloadCredentials);
    }

    public String uploadFile(MultipartFile multipartFile) {
        String fileUrl = "";
        try {
            File file = convertMultiPartToFile(multipartFile);
            String fileName = generateFileName(multipartFile);
            fileUrl = endpointUrl + "/" + bucketName + "/" + fileName;
            uploadFileTos3bucket(fileName, file);
            file.delete();
        } catch (Exception e) {
           e.printStackTrace();
        }
        return fileUrl;
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private String generateFileName(MultipartFile multiPart) {
       // return new Date().getTime() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
    	 return multiPart.getOriginalFilename().replace(" ", "_");
    }

    private void uploadFileTos3bucket(String fileName, File file) {
        s3uploadclient.putObject(new PutObjectRequest(bucketName, fileName, file));
                //.withCannedAcl(CannedAccessControlList.PublicRead));
    }

    public String deleteFileFromS3Bucket(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3uploadclient.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        return "Successfully deleted";
    }
    
   
    public S3Object downloadFile(String fileName) {    	
    	S3Object s3FileObj = s3downloadclient.getObject(new GetObjectRequest(bucketName, fileName));
        return s3FileObj;
    }         

}
