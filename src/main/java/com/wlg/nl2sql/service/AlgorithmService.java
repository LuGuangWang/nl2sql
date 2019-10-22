package com.wlg.nl2sql.service;


import com.huaban.analysis.jieba.JiebaSegmenter;
import com.wlg.nl2sql.beans.MetadataBean;
import com.wlg.nl2sql.comonent.SQLComponent;
import com.wlg.nl2sql.datas.Datas;
import com.wlg.nl2sql.tools.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 算法调用服务
 */
@Service
public class AlgorithmService {
    @Autowired
    private SQLComponent sqlCom;

    private final Logger log = LoggerFactory.getLogger(AlgorithmService.class);

    public long fields_cnt = 0;
    public static long tables_cnt = 0;


    private final String regex = "[,|，|：|；|(|)|（|）|\\[|\\]|\\s|信息|数据]";

    /**
     * 表名识别算法
     * @param msg
     * @return
     */
    private Map<String,Float> calculateTable(String msg){
        Map<String,Float> rates = new HashMap<>();

        //初始化全局参数
        initCnt();

        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> tokens = segmenter.sentenceProcess(msg);

        for(String word:tokens){
            word = word.replaceAll(regex,"");
            if(word.length()>=2){
                float p_field = calcFieldRate(word);
                if(p_field>0){
                    for(List<MetadataBean> fields: Datas.tables.values()){
                        String tableName = fields.get(0).getTableNameEn();
                        String tableCn = fields.get(0).getTableNameCn();

                        float p_field_table = fieldOfTableRate(fields,word);
                        if(p_field_table>0){
                            float p_table = calcTableRate(fields.size());
                            float p = p_field_table * p_table / p_field;
                            if(rates.containsKey(tableName)){
                                float sumP = rates.get(tableName).floatValue() + p;
                                rates.put(tableName,sumP);
                            }else{
                                rates.put(tableName,p);
                            }
                        }

                        if(tableCn!=null && tableCn.contains(word)){
                            float p_word_table = 1.0f * word.length()/(tableCn.length()+1);
                            float p_table = 1.0f/(tables_cnt+1);
                            float p_table_db = calcTableRate(word);

                            float p = p_word_table * p_table / p_table_db;

                            if(rates.containsKey(tableName)){
                                float sumP = rates.get(tableName).floatValue() + p;
                                rates.put(tableName,sumP);
                            }else{
                                rates.put(tableName,p);
                            }
                        }

                    }
                }
            }
        }

        return rates;
    }

    private List<String> getTop3Tables(Map<String, Float> rates) {
        List<String> tables = new ArrayList<>();

        String top1Table = "",top2Table = "",top3Table = "",tmpTable="",swapTable = "";
        float top1Rate = 0.0f,top2Rate = 0.0f,top3Rate = 0.0f,tmpRate=0.0f,swapRate=0.0f;

        for(Map.Entry<String,Float> item:rates.entrySet() ){
            tmpTable = item.getKey();
            tmpRate = item.getValue().floatValue();

            if(tmpRate>top1Rate){
                swapTable = tmpTable;
                swapRate = tmpRate;

                tmpTable = top1Table;
                tmpRate = top1Rate;

                top1Table = swapTable;
                top1Rate = swapRate;
            }

            if(tmpRate>top2Rate){
                swapTable = tmpTable;
                swapRate = tmpRate;

                tmpTable = top2Table;
                tmpRate = top2Rate;

                top2Table = swapTable;
                top2Rate = swapRate;
            }

            if(tmpRate>top3Rate){
                swapTable = tmpTable;
                swapRate = tmpRate;

                tmpTable = top3Table;
                tmpRate = top3Rate;

                top3Table = swapTable;
                top3Rate = swapRate;
            }
        }

        if(top1Table.length()>0){
            tables.add(top1Table);
        }
        if(top2Table.length()>0){
            tables.add(top2Table);
        }
        if(top3Table.length()>0){
            tables.add(top3Table);
        }

        System.out.println("===top1Table:"+top1Table +"===top1Rate:"+top1Rate);
        System.out.println("===top2Table:"+top2Table +"===top2Rate:"+top2Rate);
        System.out.println("===top3Table:"+top3Table +"===top3Rate:"+top3Rate);

        return tables;
    }
    //P(表|库)
    private float calcTableRate(String word){
        float rate = 0.0f;
        long tableCnt = 0;
        for(List<MetadataBean> fields: Datas.tables.values()){
            String tableCn = fields.get(0).getTableNameCn();
            if(tableCn!=null && tableCn.contains(word)){
                tableCnt ++;
            }
        }
        rate = 1.0f*(tableCnt+1)/(tables_cnt+1);
        return rate;
    }

    //P(表)
    private float calcTableRate(long tableFieldCnt){
        return tableFieldCnt * 1.0f / (fields_cnt+1);
    }
    //P(字段)
    private float calcFieldRate(String word){
        float rate = 0.0f;
        long word_cnt = 0;
        for(List<MetadataBean> fields: Datas.tables.values()){
            word_cnt += getFieldOfTableCnt(fields,word);
        }
        rate = 1.0f * (word_cnt+1)/(fields_cnt+1);
        return rate;
    }
    //P(字段|表）
    private float fieldOfTableRate(List<MetadataBean> fields,String word){
        float  rate = 0.0f;
        long cnt = getFieldOfTableCnt(fields, word);
        rate = 1.0f * cnt/(fields.size()+1);
        return rate;
    }

    private long getFieldOfTableCnt(List<MetadataBean> fields, String word) {
        long cnt = 0;
        String fieldName = null;
        for(MetadataBean bean:fields){
            fieldName = bean.getFieldNameCn();
            if(Strings.notNullOrEmpty(fieldName) && fieldName.contains(word)){
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * 获取表名算法结果
     * @param msg
     * @return
     */
    public List<MetadataBean> recallTableAndFields(String msg){
        List<MetadataBean> resFields = new ArrayList<>();


        Map<String,Float> tableRates = calculateTable(msg);
        List<String> tables = getTop3Tables(tableRates);

        resFields = calculateFields(msg,tables);

        return resFields;
    }

    private List<MetadataBean> calculateFields(String msg, List<String> tables) {
        List<MetadataBean> resFields = new ArrayList<>();
        StringBuilder fieldStr = new StringBuilder();

        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> tokens = segmenter.sentenceProcess(msg);

        for(String table:tables){
            List<MetadataBean> fields = Datas.tables.get(table);
            resFields.clear();
            fieldStr.delete(0,fieldStr.length());

            for(String word:tokens) {
                word = word.replaceAll(regex, "");
                if (word.length() >= 2) {
                    for(MetadataBean field:fields){
                        if(field.getFieldNameCn().contains(word)
                            ||field.getFieldNameEn().contains(word)){
                            if(!resFields.contains(field)) {
                                resFields.add(field);
                                fieldStr.append(field.getFieldNameEn()).append(",");
                            }
                        }
                    }
                }
            }

            if(fieldStr.length()>0){
                String tableEn = table;
                String fieldEn = fieldStr.deleteCharAt(fieldStr.length()-1).toString();
                System.out.println("===tableEn:"+tableEn + "===Fileds:"+fieldEn);
            }

        }
        return resFields;
    }

    public void initCnt(){
        long field_count = 0;
        long table_count = 0;
        for(List<MetadataBean> fields: Datas.tables.values()){
            field_count+=fields.size();
            table_count++;
        }
        this.fields_cnt = field_count;
        this.tables_cnt = table_count;
        log.info("===table cnt:"+table_count +"=== total fields count:"+ fields_cnt);
    }

}
