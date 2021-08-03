package com.dotcms.auth.providers.jwt.factories;


import com.dotmarketing.db.DbConnectionFactory;

public class ApiTokenSQL {


    protected ApiTokenSQL() {
        
    }
    
    

    private static ApiTokenSQL instance;
    
    
    public static ApiTokenSQL getInstance() {
        if (instance == null){
            instance = DbConnectionFactory.isMySql() ? new ApiTokenSQLMySQL() :
                    DbConnectionFactory.isPostgres() ? new ApiTokenSQL()
                        : DbConnectionFactory.isMsSql() ? new ApiTokenSQLMSSQL() :
                            new ApiTokenSQL();
        }
        return instance;
    }


    protected final String SELECT_BY_TOKEN_USER_ID_SQL_INACTIVE = "select * from api_token_issued where token_userid=? and (expire_date < ? or revoke_date is  not null ) order by issue_date desc";
    protected final String SELECT_BY_TOKEN_USER_ID_SQL_ACTIVE =
            "select * from api_token_issued where token_userid=? and expire_date > ? and revoke_date is null order by issue_date desc";
    protected final String SELECT_BY_TOKEN_ID_SQL = "select * from api_token_issued where token_id=?";
    protected final String UPDATE_REVOKE_TOKEN_SQL = "update api_token_issued set revoke_date=?, mod_date=? where token_id=? and revoke_date is null";
    protected final String INSERT_TOKEN_ISSUE_SQL =
            "insert into api_token_issued ( token_id, token_userid, issue_date, expire_date, requested_by_userid, requested_by_ip, revoke_date, allowed_from, issuer, claims, mod_date) values (?,?,?,?,?,?,?,?,?,?,?) ";

    protected final String DELETE_TOKEN_SQL = "delete from api_token_issued where token_id=?";
    
    public final String DROP_TOKEN_TABLE = "drop table if exists api_token_issued";
    
    public String CREATE_TOKEN_TABLE_SCRIPT() {
        return "create table api_token_issued("
                    + "token_id varchar(255) NOT NULL, "
                    + "token_userid varchar(255) NOT NULL, "
                    + "issue_date TIMESTAMP NOT NULL, "
                    + "expire_date TIMESTAMP NOT NULL, "
                    + "requested_by_userid  varchar(255) NOT NULL, "
                    + "requested_by_ip  varchar(255) NOT NULL, "
                    + "revoke_date TIMESTAMP DEFAULT NULL, "
                    + "allowed_from  varchar(255) , "
                    + "issuer  varchar(255) , "
                    + "claims  text , "
                    + "mod_date  TIMESTAMP NOT NULL, "
                    + "PRIMARY KEY (token_id));"
                    + "create index idx_api_token_issued_user ON api_token_issued (token_userid)";
    
    }
    

    
}
