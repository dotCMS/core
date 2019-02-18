package com.dotcms.auth.providers.jwt.factories;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dotcms.auth.providers.jwt.beans.JWTokenIssued;
import com.dotcms.util.transform.DBTransformer;


public class JWTokenDBTransformer implements DBTransformer<JWTokenIssued>{

    
    private final List<JWTokenIssued> tokenList;
    public JWTokenDBTransformer(final List<Map<String, Object>> dbMapList) {
        tokenList = dbMapList
                .stream()
                .map(db->fromMap(db))
                .collect(Collectors.toList());
    }

    
    @Override
    public List<JWTokenIssued> asList() {

        return tokenList;
    }
    /**
     *  final static String BASE_SCRIPT = "create table jwt_token_issue("
                + "token_id varchar(255) NOT NULL, "
                + "token_userid varchar(255) NOT NULL, "
                + "issue_date TIMESTAMP NOT NULL, "
                + "expire_date TIMESTAMP NOT NULL, "
                + "requested_by_userid  varchar(255) NOT NULL, "
                + "requested_by_ip  varchar(255) NOT NULL, "
                + "revoke_date  TIMESTAMP, "
                + "allowed_from  varchar(255) , "
                + "cluster_id  varchar(255) , "
                + "meta_data  text , "
                + "mod_date  TIMESTAMP NOT NULL, "
                + "PRIMARY KEY (token_id));\n"
                + "create index idx_jwt_token_issue_user ON jwt_token_issue (token_userid);";
     *
     */
    private JWTokenIssued fromMap(Map<String, Object> dbMap) {
        
        return JWTokenIssued
                .builder()
                .withId((String) dbMap.get("token_id"))
                .withUserId((String) dbMap.get("token_userid"))
                .withIssueDate((Date) dbMap.get("issue_date"))
                .withExpires((Date) dbMap.get("expire_date"))
                .withRequestingUserId((String) dbMap.get("requested_by_userid"))
                .withRequestingIp((String) dbMap.get("requested_by_ip"))
                .withRevoked((Date) dbMap.get("revoke_date"))
                .withAllowFromNetwork((String) dbMap.get("allowed_from"))
                .withClusterId((String) dbMap.get("cluster_id"))
                .withMetaData((String) dbMap.get("meta_data"))
                .withModDate((Date) dbMap.get("mod_date"))
                .build();

    }

    
    
    
    
    
    
}
