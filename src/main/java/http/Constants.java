package http;

/**
 * Created by zengtao on 2016/11/17.
 */
public class Constants {

    public static final int SOCKET_TIMEOUT = 60 * 1000;
    public static final int CONNECTION_TIMEOUT = 30 * 1000;
    public static final int MAX_CONNECTIONS = 100;
    public static final String CHARSET = "UTF-8";
    public static final String PHONE = "phone";
    public static final int MAX_ADD_BATCH_SIZE = 1000;

    public static final String QUERY_USER = "userMapper.queryUser";
    public static final String ADD_USER = "userMapper.addUser";

    public static final String INSERT_TOKEN = "tokenMapper.addToken";
    public static final String QUERY_TOKEN_TYPE = "tokenMapper.queryTokenType";

    public static final String QUERY_CONTEXT_TYPE = "contextTypeMapper.queryContextType";
    public static final String ADD_CONTEXT_TYPE = "contextTypeMapper.addContextType";

    public static final String ADD_USER_ATTRIBUTE = "userAttributeMapper.addUserType";

    public static final String QUERY_TYPE = "typeDictionaryMapper.queryType";

    public static final String SUCCESS_CODE = "0000";


}
