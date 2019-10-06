package com.example.demo.controller;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.gridfs.GridFSDBFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class HelloController {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private MongoDbFactory mongoDbFactory;

    private Map<String,String> map=new HashMap<>();

    @RequestMapping("/upload")
    public String upload(MultipartFile file){
        ObjectId objectId=null;
        System.out.println(file.getContentType());
        System.out.println(file.getName());
        System.out.println(file.getOriginalFilename());

        try{
            objectId=gridFsTemplate.store(file.getInputStream(),file.getOriginalFilename(),file.getContentType());
            map.put("fileId",objectId.toString());
        }catch (Exception e){
            e.printStackTrace();
        }

        assert objectId != null;
        return objectId.toString();
    }

    @RequestMapping("/get")
    public void get(HttpServletResponse response){
        Query query=Query.query(Criteria.where("_id").is(map.get("fileId")));
        GridFSFile gridFSFile=gridFsTemplate.findOne(query);

        assert gridFSFile != null;
        GridFsResource gridFsResource=new GridFsResource(gridFSFile,
                GridFSBuckets.create(mongoDbFactory.getDb()).openDownloadStream(gridFSFile.getObjectId()));

        response.reset();
        try{
            //response.setContentType("application/octet-stream");
            response.setContentLength((int)gridFSFile.getLength());
            response.setHeader("Content-Disposition","attachment;filename="+ URLEncoder.encode(Objects.requireNonNull(gridFsResource.getFilename()),"utf-8"));
            IOUtils.copy(gridFsResource.getInputStream(),response.getOutputStream());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @RequestMapping("/delete")
    public String delete(){
        Query query=Query.query(Criteria.where("_id").is(map.get("fileId")));
        gridFsTemplate.delete(query);

        return "success";
    }
}
