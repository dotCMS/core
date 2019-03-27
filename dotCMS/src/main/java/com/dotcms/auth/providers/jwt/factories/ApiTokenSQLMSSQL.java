package com.dotcms.auth.providers.jwt.factories;

public class ApiTokenSQLMSSQL extends ApiTokenSQL{

        @Override
        public String CREATE_TOKEN_TABLE_SCRIPT() {
            return "CREATE TABLE api_token_issued( "
                    + "token_id NVARCHAR(255) NOT NULL, "
                    + "token_userid NVARCHAR(255) NOT NULL, "
                    + "issue_date datetime NOT NULL, "
                    + "expire_date datetime NOT NULL, "
                    + "requested_by_userid NVARCHAR(255) NOT NULL, "
                    + "requested_by_ip NVARCHAR(255) NOT NULL, "
                    + "revoke_date datetime DEFAULT NULL, "
                    + "allowed_from NVARCHAR(255) , "
                    + "issuer NVARCHAR(255) , "
                    + "claims NVARCHAR(MAX) , "
                    + "mod_date datetime NOT NULL, "
                    + "PRIMARY KEY (token_id) ); "
                    + "create index idx_api_token_issued_user ON api_token_issued (token_userid)";
        
        }
    }
    
    
