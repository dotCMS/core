package com.dotcms.auth.providers.jwt.factories;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.util.transform.DBTransformer;


public class ApiTokenDBTransformer implements DBTransformer<ApiToken>{

    
    private final List<ApiToken> tokenList;
    public ApiTokenDBTransformer(final List<Map<String, Object>> dbMapList) {
        tokenList = dbMapList
                .stream()
                .map(db->fromMap(db))
                .collect(Collectors.toList());
    }

    
    @Override
    public List<ApiToken> asList() {

        return tokenList;
    }

    private ApiToken fromMap(Map<String, Object> dbMap) {
        
        return ApiToken
                .builder()
                .withId((String) dbMap.get("token_id"))
                .withUserId((String) dbMap.get("token_userid"))
                .withIssueDate((Date) dbMap.get("issue_date"))
                .withExpires((Date) dbMap.get("expire_date"))
                .withRequestingUserId((String) dbMap.get("requested_by_userid"))
                .withRequestingIp((String) dbMap.get("requested_by_ip"))
                .withRevoked((Date) dbMap.get("revoke_date"))
                .withAllowNetwork((String) dbMap.get("allowed_from"))
                .withIssuer((String) dbMap.get("issuer"))
                .withClaims((String) dbMap.get("claims"))
                .withModDate((Date) dbMap.get("mod_date"))
                .build();

    }

    
    
    
    
    
    
}
