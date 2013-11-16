package com.hackathon.relevantXKCD;

import java.io.IOException;

import javax.servlet.http.*;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.hackathon.relevantXKCD.Global;

@SuppressWarnings("serial")
public class RelevantXKCDServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		System.out.println("RelevantXKCDServlet DoGet");
		
		String action = req.getParameter("action");
		String query = req.getParameter("query");
		System.out.println("Action: "+action+", query: "+query);
		
		if(action.equals("rebuild")) {
			UserService userService = UserServiceFactory.getUserService();
			User newUser = userService.getCurrentUser();
			if (newUser == null) {
				return;
			}
			String user = newUser.getNickname();
			Global.clearAllCaches();
		} else if(action.equals("xkcd")) {
			System.out.println("Query: "+query);
			resp.getWriter().println("123");
		}
	}
}
