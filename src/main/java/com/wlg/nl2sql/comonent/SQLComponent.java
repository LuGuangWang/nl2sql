package com.wlg.nl2sql.comonent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SQLComponent {

    private final Logger log = LoggerFactory.getLogger(SQLComponent.class);

    /**
     * build sql
     * @param tableEn
     * @param fieldEn
     * @return
     */
    public String buildSql(String tableEn,String fieldEn){
        StringBuilder sql = new StringBuilder("select");
        sql.append(" ").append(fieldEn).append(" ");
        sql.append("from ").append(tableEn);
        sql.append(" limit 10");

        log.info("=== sql:"+sql.toString());

        return sql.toString();
    }
}
