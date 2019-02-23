package com.dotcms.auth.providers.jwt.factories;

public class ApiTokenSQL {

    
    
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
    
    private ApiTokenSQL() {


    }

    private static ApiTokenSQL instance;
    
    
    protected static ApiTokenSQL getInstance() {
        if(instance==null) {
            instance = new ApiTokenSQL();
        }
        return instance;
    }


    protected String SELECT_BY_TOKEN_USER_ID_SQL_ALL = "select * from jwt_token_issued where token_userid=? order by issue_date desc";
    protected String SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE =
            "select * from jwt_token_issued where token_userid=? and reoke_date is null order by issue_date desc";
    protected String SELECT_BY_TOKEN_ID_SQL = "select * from jwt_token_issued where token_id=?";
    protected String UPDATE_REVOKE_TOKEN_SQL = "update jwt_token_issued set revoke_date=?, mod_date=? where token_id=?";
    protected String INSERT_TOKEN_ISSUE_SQL =
            "insert into jwt_token_issued ( token_id, token_userid, issue_date, expire_date, requested_by_userid, requested_by_ip, revoke_date, allowed_from, cluster_id, meta_data, mod_date) values (?,?,?,?,?,?,?,?,?,?,?) ";


}
