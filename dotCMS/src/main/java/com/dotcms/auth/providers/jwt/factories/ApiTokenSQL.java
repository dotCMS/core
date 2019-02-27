package com.dotcms.auth.providers.jwt.factories;

public class ApiTokenSQL {

    
    private ApiTokenSQL() {


    }

    private static ApiTokenSQL instance;
    
    
    public static ApiTokenSQL getInstance() {
        if(instance==null) {
            instance = new ApiTokenSQL();
        }
        return instance;
    }


    protected String SELECT_BY_TOKEN_USER_ID_SQL_ALL = "select * from api_token_issued where token_userid=? order by issue_date desc";
    protected String SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE =
            "select * from api_token_issued where token_userid=? and revoke_date is null order by issue_date desc";
    protected String SELECT_BY_TOKEN_ID_SQL = "select * from api_token_issued where token_id=?";
    protected String UPDATE_REVOKE_TOKEN_SQL = "update api_token_issued set revoke_date=?, mod_date=? where token_id=?";
    protected String INSERT_TOKEN_ISSUE_SQL =
            "insert into api_token_issued ( token_id, token_userid, issue_date, expire_date, requested_by_userid, requested_by_ip, revoke_date, allowed_from, cluster_id, claims, mod_date) values (?,?,?,?,?,?,?,?,?,?,?) ";

    protected String DELETE_TOKEN_SQL = "delete from api_token_issued where token_id=?";
    
    public String DROP_TOKEN_TABLE = "drop table if exists api_token_issued";
    
    public String CREATE_TOKEN_TABLE_SCRIPT = "create table api_token_issued("
                    + "token_id varchar(255) NOT NULL, "
                    + "token_userid varchar(255) NOT NULL, "
                    + "issue_date TIMESTAMP NOT NULL, "
                    + "expire_date TIMESTAMP NOT NULL, "
                    + "requested_by_userid  varchar(255) NOT NULL, "
                    + "requested_by_ip  varchar(255) NOT NULL, "
                    + "revoke_date TIMESTAMP NULL DEFAULT NULL, "
                    + "allowed_from  varchar(255) , "
                    + "cluster_id  varchar(255) , "
                    + "claims  text , "
                    + "mod_date  TIMESTAMP NOT NULL, "
                    + "PRIMARY KEY (token_id));"
                    + "create index idx_api_token_issued_user ON api_token_issued (token_userid)";
    
}
