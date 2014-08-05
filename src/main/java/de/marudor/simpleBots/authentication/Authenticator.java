package de.marudor.simpleBots.authentication;

import de.marudor.simpleBots.exceptions.InvalidLoginException;

import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.util.List;
import java.util.Map;

/**
 * Created by marudor on 03/08/14.
 */
public class Authenticator {


    public static boolean isAllowed(String username, String password, Task task) throws InvalidLoginException {
        AuthUser user = AuthUser.getByUsername(username);
        if (user == null || !user.getPassword().equals(password)) throw new InvalidLoginException();
        switch (task) {
            case TWEET:
            case REGISTER:
            case GET_STATUS:
            case GET_ACCOUNT:
            case GET_ACCOUNTS:
                return true;
            case CREATE_API_USER:
                return user.isAdmin();
            default:
                return false;
        }
    }

    public static boolean isAllowed(WebServiceContext wsctx, Task task) throws InvalidLoginException {
        MessageContext mctx = wsctx.getMessageContext();
        Map http_headers = (Map) mctx.get(MessageContext.HTTP_REQUEST_HEADERS);
        List userList = (List) http_headers.get("Username");
        List passList = (List) http_headers.get("Password");
        String username = userList!=null ? userList.get(0).toString() : null;
        String password = passList!=null ? passList.get(0).toString() : null;
        if (userList==null || password == null) throw new InvalidLoginException();
        return isAllowed(username, password, task);
    }
}
