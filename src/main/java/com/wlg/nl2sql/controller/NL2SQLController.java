package com.wlg.nl2sql.controller;

import com.wlg.nl2sql.beans.MetadataBean;
import com.wlg.nl2sql.service.AlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NL2SQLController {

    @Autowired
    private AlgorithmService algorith;

    @GetMapping("nl2sql")
    public String nl2Sql(String nlStr){
        //将语句传输给算法
        List<MetadataBean> fields = algorith.recallTableAndFields(nlStr);

        return "请问您想要什么数据？";
    }

}
