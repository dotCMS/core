package com.dotcms.auth.providers.jwt.factories;

public class ApiTokenSQLMySQL extends ApiTokenSQL{

        @Override
        public String CREATE_TOKEN_TABLE_SCRIPT() {
            return "create table api_token_issued("
                + "token_id varchar(255) NOT NULL, "
                + "token_userid varchar(255) NOT NULL, "
                + "issue_date TIMESTAMP NOT NULL default CURRENT_TIMESTAMP, "
                + "expire_date TIMESTAMP NOT NULL default CURRENT_TIMESTAMP, "
                + "requested_by_userid  varchar(255) NOT NULL, "
                + "requested_by_ip  varchar(255) NOT NULL, "
                + "revoke_date TIMESTAMP NULL DEFAULT NULL, "
                + "allowed_from  varchar(255) , "
                + "issuer  varchar(255) , "
                + "claims  text , "
                + "mod_date  TIMESTAMP NOT NULL default CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (token_id));"
                + "create index idx_api_token_issued_user ON api_token_issued (token_userid)";
        
        }
    }
    
    
